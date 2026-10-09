package com.xqy.sms.ai.infrastructure.service.knowledge;

import com.xqy.sms.ai.api.model.KnowledgeIngestionCommand;
import com.xqy.sms.common.security.rpc.InternalCallSigner;
import com.xqy.sms.system.api.service.SystemManagementService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@EnabledIfEnvironmentVariable(named="SMS_TEST_MYSQL_URL",matches="jdbc:mysql:.*")
class KnowledgeQueueMySqlTest {
    JdbcTemplate jdbc;
    KnowledgeIngestionQueue queue;
    KnowledgeDocumentIngestionService ingestion;
    @BeforeEach void setup() {
        String url=System.getenv("SMS_TEST_MYSQL_URL");
        if(!url.matches("jdbc:mysql://[^/]+/sms_codex_verify_[a-z0-9_]+(?:\\?.*)?")) throw new IllegalArgumentException("Disposable schema required");
        var source=new DriverManagerDataSource(url,System.getenv("SMS_TEST_MYSQL_USERNAME"),System.getenv("SMS_TEST_MYSQL_PASSWORD"));
        jdbc=new JdbcTemplate(source);
        clean();
        jdbc.update("INSERT INTO ai_knowledge_base(id,tenant_id,name) VALUES(9901,'queue-test','test')");
        jdbc.update("INSERT INTO ai_knowledge_document(id,tenant_id,knowledge_base_id,document_no,document_name,version) VALUES(9902,'queue-test',9901,'test','test','v1')");
        jdbc.update("INSERT INTO ai_knowledge_document_version(id,tenant_id,document_id,version,chunk_count) VALUES(9903,'queue-test',9902,'v1',2)");
        jdbc.update("INSERT INTO ai_knowledge_document_detail(id,tenant_id,document_id,document_version_id,chunk_no,content) VALUES(9904,'queue-test',9902,9903,1,'first'),(9905,'queue-test',9902,9903,2,'second')");
        ingestion=mock(KnowledgeDocumentIngestionService.class);
        when(ingestion.ingestAfter(any(),anyInt())).thenReturn(new KnowledgeDocumentIngestionService.IngestionResult(2,2,1));
        queue=new KnowledgeIngestionQueue(jdbc,ingestion,new DataSourceTransactionManager(source));
        ReflectionTestUtils.setField(queue,"authorization",mock(SystemManagementService.class));
    }
    @AfterEach void clean() {
        if(jdbc==null) return;
        for(String table:List.of("ai_knowledge_ingestion_job","ai_knowledge_document_detail","ai_knowledge_document_version","ai_knowledge_document","ai_knowledge_base"))
            jdbc.update("DELETE FROM "+table+" WHERE tenant_id='queue-test'");
    }
    String enqueue(int revision) {
        var caller=new InternalCallSigner("01234567890123456789012345678901").sign("sms-web-bff","queue-test",1L,2L,"request");
        return queue.enqueue(new KnowledgeIngestionCommand(null,9903L,null,revision,"queue-test",caller)).getJobIds().getFirst();
    }
    Map<String,Object> claim(String id,String token) {
        jdbc.update("UPDATE ai_knowledge_ingestion_job SET status='RUNNING',worker_token=?,lease_expire_time=DATE_ADD(NOW(),INTERVAL 120 SECOND) WHERE id=?",token,id);
        return jdbc.queryForMap("SELECT * FROM ai_knowledge_ingestion_job WHERE id=?",id);
    }
    String state(String id) { return jdbc.queryForObject("SELECT status FROM ai_knowledge_ingestion_job WHERE id=?",String.class,id); }
    @Test void enqueuesWithoutEmbeddingAndActivatesOnlyAfterAllChunksSucceed() {
        String id=enqueue(1);
        verifyNoInteractions(ingestion);
        assertEquals("PENDING",state(id));
        queue.execute(claim(id,"worker"));
        assertEquals("SUCCEEDED",state(id));
        assertEquals(100,jdbc.queryForObject("SELECT progress FROM ai_knowledge_ingestion_job WHERE id=?",Integer.class,id));
        verify(ingestion).activate(9903L,1,"queue-test");
        assertEquals(id,enqueue(1));
        assertEquals("INDEXED",jdbc.queryForObject("SELECT index_status FROM ai_knowledge_document_version WHERE id=9903",String.class));
    }
    @Test void cancellationAndLostLeaseCannotActivateOrOverwriteNewWorker() {
        String id=enqueue(1); var old=claim(id,"old");
        jdbc.update("UPDATE ai_knowledge_ingestion_job SET worker_token='new' WHERE id=?",id);
        queue.execute(old);
        verifyNoInteractions(ingestion);
        assertEquals("RUNNING",state(id));
        var current=jdbc.queryForMap("SELECT * FROM ai_knowledge_ingestion_job WHERE id=?",id);
        jdbc.update("UPDATE ai_knowledge_ingestion_job SET cancel_requested=1 WHERE id=?",id);
        queue.execute(current);
        assertEquals("CANCELLED",state(id));
        verifyNoInteractions(ingestion);
    }
    @Test void fencedFailureDoesNotClobberNewWorkerSuccess() {
        String id=enqueue(1); var old=claim(id,"old");
        when(ingestion.ingestAfter(any(),anyInt())).thenAnswer(call->{
            jdbc.update("UPDATE ai_knowledge_ingestion_job SET worker_token='new',status='SUCCEEDED' WHERE id=?",id);
            jdbc.update("UPDATE ai_knowledge_document_version SET index_status='INDEXED',active_index_revision='1' WHERE id=9903");
            throw new IllegalStateException("upstream failure");
        });
        queue.execute(old);
        assertEquals("SUCCEEDED",state(id));
        assertEquals("INDEXED",jdbc.queryForObject("SELECT index_status FROM ai_knowledge_document_version WHERE id=9903",String.class));
        verify(ingestion,never()).activate(anyLong(),anyInt(),anyString());
    }
    @Test void olderRevisionFinishingAfterNewerOneDoesNotRollBackIndex() {
        String first=enqueue(1), second=enqueue(2);
        queue.execute(claim(second,"new"));
        queue.execute(claim(first,"old"));
        verify(ingestion).activate(9903L,2,"queue-test");
        verify(ingestion,never()).activate(9903L,1,"queue-test");
        assertEquals("2",jdbc.queryForObject("SELECT active_index_revision FROM ai_knowledge_document_version WHERE id=9903",String.class));
        assertEquals("SUCCEEDED",state(first));
    }
    @Test void recordsFailureWithoutActivatingPartialImport() {
        String id=enqueue(1);
        when(ingestion.ingestAfter(any(),anyInt())).thenThrow(new IllegalStateException("upstream unavailable"));
        queue.execute(claim(id,"worker"));
        assertEquals("FAILED",state(id));
        verify(ingestion,never()).activate(anyLong(),anyInt(),anyString());
    }
}
