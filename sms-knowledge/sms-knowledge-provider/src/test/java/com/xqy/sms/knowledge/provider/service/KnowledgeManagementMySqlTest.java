package com.xqy.sms.knowledge.provider.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xqy.sms.common.exception.*;
import com.xqy.sms.common.security.rpc.*;
import com.xqy.sms.system.api.service.SystemManagementService;
import com.xqy.sms.knowledge.api.entity.AiknowledgeChunk;
import com.xqy.sms.knowledge.api.model.KnowledgeVectorQuery;
import com.xqy.sms.knowledge.provider.mapper.*;
import com.xqy.sms.knowledge.provider.milvus.MilvusVectorStore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.*;
import javax.sql.DataSource;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="SMS_TEST_MYSQL_URL",matches="jdbc:mysql:.*")
@SpringJUnitConfig(KnowledgeManagementMySqlTest.Config.class)
@Transactional
class KnowledgeManagementMySqlTest {
    static Path uploads;
    @Autowired KnowledgeManagementServiceImpl service;
    @Autowired JdbcTemplate jdbc;
    static final InternalCallContext CALLER=new InternalCallSigner("01234567890123456789012345678901").sign("sms-web-bff","knowledge-test",1L,2L,"request");
    @BeforeEach void setup() {
        jdbc.update("INSERT INTO ai_knowledge_base(id,tenant_id,name,chunk_size,chunk_overlap,split_strategy) VALUES(9801,'knowledge-test','test',100,20,'FIXED'),(9802,'other','other',100,20,'FIXED')");
    }
    @AfterAll static void removeEmptyUploadDirectory() throws Exception { if(uploads!=null) Files.deleteIfExists(uploads); }
    Map<String,Object> upload(Long document) { return service.upload(9801L,document,"test.md","文档😀".repeat(100).getBytes(StandardCharsets.UTF_8),CALLER); }
    @Test void uploadCreatesChunksAndDeduplicatesOnlyMatchingContentAndStrategy() {
        var first=upload(null);
        long version=Long.parseLong(first.get("id").toString()), document=Long.parseLong(first.get("documentId").toString());
        assertEquals("STAGED",first.get("indexStatus"));
        assertEquals(first.get("id"),upload(document).get("id"));
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM ai_knowledge_document_detail WHERE document_version_id=?",Integer.class,version)>1);
        jdbc.update("UPDATE ai_knowledge_base SET split_strategy='PARAGRAPH' WHERE id=9801");
        assertNotEquals(first.get("id"),upload(document).get("id"));
        assertThrows(ManagementNotFoundException.class,()->service.upload(9802L,null,"a.txt",new byte[]{1},CALLER));
        assertThrows(ManagementNotFoundException.class,()->service.save("bases",9802L,Map.of("name","x"),CALLER));
        assertFalse(first.containsKey("sourceUri"));
    }
    @Test void queuedTaskCanBeCancelledRetriedAndProtectsDocumentDeletion() {
        var version=upload(null);
        long document=Long.parseLong(version.get("documentId").toString());
        jdbc.update("INSERT INTO ai_knowledge_ingestion_job(id,tenant_id,document_version_id,index_revision,job_type,status) VALUES(9803,'knowledge-test',?,'1','EMBEDDING','PENDING')",version.get("id"));
        assertThrows(ManagementConflictException.class,()->service.delete("documents",document,CALLER));
        service.cancelJob(9803L,CALLER);
        assertEquals("CANCELLED",jdbc.queryForObject("SELECT status FROM ai_knowledge_ingestion_job WHERE id=9803",String.class));
        service.retryJob(9803L,CALLER);
        assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM ai_knowledge_ingestion_job WHERE id=9803",String.class));
        assertEquals(0,jdbc.queryForObject("SELECT processed_chunks FROM ai_knowledge_ingestion_job WHERE id=9803",Integer.class));
        service.cancelJob(9803L,CALLER);
        service.delete("documents",document,CALLER);
        assertThrows(ManagementConflictException.class,()->service.retryJob(9803L,CALLER));
    }
    @Test void retrievalUsesLiveDocumentAndKnowledgeBaseState() {
        var uploaded=upload(null);
        long version=Long.parseLong(uploaded.get("id").toString());
        long document=Long.parseLong(uploaded.get("documentId").toString());
        var vector=mock(MilvusVectorStore.class);
        var retrieval=new KnowledgeServiceImpl(mock(AiKnowledgeBaseMapper.class),mock(AiKnowledgeDocumentMapper.class),
                mock(AiKnowledgeDocumentVersionMapper.class),mock(AiKnowledgeIngestionJobMapper.class),vector,
                "01234567890123456789012345678901");
        ReflectionTestUtils.setField(retrieval,"metadata",jdbc);
        var query=new KnowledgeVectorQuery();
        query.setTenantId("knowledge-test");
        query.setCallerContext(new InternalCallSigner("01234567890123456789012345678901")
                .sign("sms-ai-provider","knowledge-test",1L,2L,"retrieval"));
        var chunk=new AiknowledgeChunk();
        chunk.setTenantId("knowledge-test"); chunk.setDocumentVersionId(version);
        chunk.setIndexRevision("2"); chunk.setStatus("ACTIVE");
        when(vector.search(query)).thenReturn(List.of(chunk));
        assertTrue(retrieval.queryVector(query).isEmpty());
        verifyNoInteractions(vector);
        jdbc.update("UPDATE ai_knowledge_document_version SET active_index_revision='2' WHERE id=?",version);
        assertEquals(List.of(chunk),retrieval.queryVector(query));
        assertEquals(Map.of(version,"2"),query.getActiveVersions());
        clearInvocations(vector);
        jdbc.update("UPDATE ai_knowledge_base SET enabled=0 WHERE id=9801");
        assertTrue(retrieval.queryVector(query).isEmpty());
        verifyNoInteractions(vector);
        jdbc.update("UPDATE ai_knowledge_base SET enabled=1 WHERE id=9801");
        service.delete("documents",document,CALLER);
        assertTrue(retrieval.queryVector(query).isEmpty());
        verifyNoInteractions(vector);
    }
    @Configuration @EnableTransactionManagement(proxyTargetClass=true)
    static class Config {
        @Bean DataSource source() {
            String url=System.getenv("SMS_TEST_MYSQL_URL");
            if(!url.matches("jdbc:mysql://[^/]+/sms_codex_verify_[a-z0-9_]+(?:\\?.*)?")) throw new IllegalArgumentException("Disposable schema required");
            return new DriverManagerDataSource(url,System.getenv("SMS_TEST_MYSQL_USERNAME"),System.getenv("SMS_TEST_MYSQL_PASSWORD"));
        }
        @Bean JdbcTemplate jdbc(DataSource source) { return new JdbcTemplate(source); }
        @Bean DataSourceTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }
        @Bean KnowledgeManagementServiceImpl management(JdbcTemplate jdbc) throws Exception {
            uploads=Files.createTempDirectory("sms-knowledge-test-");
            var service=new KnowledgeManagementServiceImpl(jdbc,new DocumentTextParser(),new DocumentChunker(),new ObjectMapper(),uploads.toString());
            ReflectionTestUtils.setField(service,"authorization",mock(SystemManagementService.class)); return service;
        }
    }
}
