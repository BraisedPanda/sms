package com.xqy.sms.ai.infrastructure.service.knowledge;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.xqy.sms.ai.api.model.KnowledgeIngestionCommand;
import com.xqy.sms.ai.api.model.KnowledgeIngestionResult;
import com.xqy.sms.common.exception.ManagementConflictException;
import com.xqy.sms.common.exception.ManagementNotFoundException;
import com.xqy.sms.common.security.rpc.InternalCallContext;
import com.xqy.sms.system.api.service.SystemManagementService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/** Durable database queue, claimed with a lease and fenced by a unique worker token. */
@Service
public class KnowledgeIngestionQueue {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionQueue.class);
    @DubboReference(check = false) private SystemManagementService authorization;
    private final JdbcTemplate jdbc;
    private final KnowledgeDocumentIngestionService ingestion;
    private final TransactionTemplate transaction;

    public KnowledgeIngestionQueue(JdbcTemplate jdbc, KnowledgeDocumentIngestionService ingestion, PlatformTransactionManager manager) {
        this.jdbc = jdbc; this.ingestion = ingestion; this.transaction = new TransactionTemplate(manager);
    }

    public KnowledgeIngestionResult enqueue(KnowledgeIngestionCommand command) {
        authorization.authorize("knowledge:ingestion:create", command.callerContext());
        if (command.limit() != null && (command.limit() < 1 || command.limit() > 10000)) throw new IllegalArgumentException("limit 须为 1–10000");
        if (command.indexRevision() != null && command.indexRevision() <= 0) throw new IllegalArgumentException("索引版本必须为正整数");
        return transaction.execute(status -> {
            String sql = "SELECT v.id,v.document_id,v.chunk_count FROM ai_knowledge_document_version v JOIN ai_knowledge_document d ON d.id=v.document_id AND d.tenant_id=v.tenant_id JOIN ai_knowledge_base b ON b.id=d.knowledge_base_id AND b.tenant_id=d.tenant_id WHERE v.tenant_id=? AND d.status='ACTIVE' AND b.enabled=1";
            List<Object> args = new ArrayList<>(); args.add(command.tenantId());
            if (command.knowledgeBaseId() != null) { sql += " AND d.knowledge_base_id=?"; args.add(command.knowledgeBaseId()); }
            if (command.documentVersionId() != null) { sql += " AND v.id=?"; args.add(command.documentVersionId()); }
            if (command.documentVersionId() == null) sql += " AND v.version=d.version";
            List<Map<String, Object>> versions = jdbc.queryForList(sql + " ORDER BY v.id LIMIT 201", args.toArray());
            if (versions.size() > 200) throw new IllegalArgumentException("每次最多排队 200 个文档版本，请按知识库或版本分批提交");
            if (versions.isEmpty()) throw new ManagementNotFoundException();
            List<String> ids = new ArrayList<>(); int lastRevision = 0;
            for (Map<String, Object> version : versions) {
                Long versionId = ((Number) version.get("id")).longValue();
                jdbc.queryForObject("SELECT id FROM ai_knowledge_document WHERE id=? AND tenant_id=? FOR UPDATE", Long.class, version.get("document_id"), command.tenantId());
                int chunks = jdbc.queryForObject("SELECT COUNT(*) FROM ai_knowledge_document_detail WHERE document_version_id=? AND tenant_id=?", Integer.class, versionId, command.tenantId());
                if (chunks == 0) throw new IllegalArgumentException("文档没有分块，请先上传或解析文档");
                Integer max = jdbc.queryForObject("SELECT COALESCE(MAX(CAST(index_revision AS UNSIGNED)),0) FROM ai_knowledge_ingestion_job WHERE document_version_id=? AND tenant_id=?", Integer.class, versionId, command.tenantId());
                int revision = command.indexRevision() == null ? Math.addExact(max == null ? 0 : max, 1) : command.indexRevision();
                List<String> existing = jdbc.queryForList("SELECT CAST(id AS CHAR) FROM ai_knowledge_ingestion_job WHERE tenant_id=? AND document_version_id=? AND index_revision=? AND job_type='EMBEDDING'", String.class, command.tenantId(), versionId, String.valueOf(revision));
                if (!existing.isEmpty()) ids.add(existing.getFirst());
                else {
                    long id = IdWorker.getId();
                    jdbc.update("INSERT INTO ai_knowledge_ingestion_job (id,tenant_id,document_version_id,index_revision,job_type,status,progress,processed_chunks,create_by,modify_by) VALUES (?,?,?,?,'EMBEDDING','PENDING',0,0,?,?)",
                            id, command.tenantId(), versionId, String.valueOf(revision), command.callerContext().userId().toString(), command.callerContext().userId().toString());
                    ids.add(String.valueOf(id));
                    jdbc.update("UPDATE ai_knowledge_document_version SET chunk_count=?,index_status='PENDING',error_message=NULL WHERE id=? AND tenant_id=?", chunks, versionId, command.tenantId());
                }
                lastRevision = revision;
            }
            KnowledgeIngestionResult result = new KnowledgeIngestionResult(versions.size(), 0, lastRevision);
            result.setJobIds(ids); result.setStatus("QUEUED"); return result;
        });
    }

    @Scheduled(fixedDelayString = "${sms.ai.knowledge-ingestion.poll-delay-ms:1000}")
    public void poll() {
        Map<String, Object> job = transaction.execute(status -> {
            List<Map<String, Object>> jobs = jdbc.queryForList("SELECT * FROM ai_knowledge_ingestion_job WHERE job_type='EMBEDDING' AND (status='PENDING' OR (status='RUNNING' AND lease_expire_time<CURRENT_TIMESTAMP(3))) ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED");
            if (jobs.isEmpty()) return null;
            Map<String, Object> selected = jobs.getFirst(); String token = UUID.randomUUID().toString();
            jdbc.update("UPDATE ai_knowledge_ingestion_job SET status='RUNNING',worker_token=?,lease_expire_time=DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 120 SECOND),start_time=COALESCE(start_time,CURRENT_TIMESTAMP(3)),retry_count=retry_count+? WHERE id=?", token, "RUNNING".equals(selected.get("status")) ? 1 : 0, selected.get("id"));
            selected.put("worker_token", token); return selected;
        });
        if (job != null) execute(job);
    }

    void execute(Map<String, Object> job) {
        long id = ((Number) job.get("id")).longValue(), version = ((Number) job.get("document_version_id")).longValue();
        String tenant = job.get("tenant_id").toString(), token = job.get("worker_token").toString();
        int revision = Integer.parseInt(job.get("index_revision").toString());
        try {
            int count = jdbc.queryForObject("SELECT COUNT(*) FROM ai_knowledge_document_detail x JOIN ai_knowledge_document d ON d.id=x.document_id AND d.tenant_id=x.tenant_id WHERE x.document_version_id=? AND x.tenant_id=? AND d.status='ACTIVE'", Integer.class, version, tenant);
            if (count == 0) throw new IllegalStateException("文档已删除或缺少分块");
            Integer lastChunk = jdbc.queryForObject("SELECT MAX(chunk_no) FROM ai_knowledge_document_detail WHERE document_version_id=? AND tenant_id=?", Integer.class, version, tenant);
            Integer firstChunk = jdbc.queryForObject("SELECT MIN(chunk_no) FROM ai_knowledge_document_detail WHERE document_version_id=? AND tenant_id=?", Integer.class, version, tenant);
            if (lastChunk == null || lastChunk != count || firstChunk == null || firstChunk != 1)
                throw new IllegalStateException("分块序号须从 1 连续递增");
            int offset = ((Number) job.get("processed_chunks")).intValue();
            while (offset < count) {
                if (!renew(id, token)) { cancelIfOwned(id, token); return; }
                var result = ingestion.ingestAfter(new KnowledgeDocumentIngestionService.IngestionRequest(null, version, 32, revision, tenant), offset);
                if (result.imported() == 0) throw new IllegalStateException("分块序号或内容不完整");
                offset += result.imported();
                if (jdbc.update("UPDATE ai_knowledge_ingestion_job SET processed_chunks=?,progress=?,lease_expire_time=DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 120 SECOND) WHERE id=? AND worker_token=? AND status='RUNNING' AND cancel_requested=0", offset, Math.min(99, offset * 100 / count), id, token) != 1) { cancelIfOwned(id, token); return; }
            }
            transaction.executeWithoutResult(status -> {
                List<Map<String, Object>> owned = jdbc.queryForList("SELECT id FROM ai_knowledge_ingestion_job WHERE id=? AND worker_token=? AND status='RUNNING' AND cancel_requested=0 FOR UPDATE", id, token);
                if (owned.isEmpty()) { cancelIfOwned(id, token); return; }
                Map<String, Object> document = jdbc.queryForMap("SELECT d.id,d.version,v.version AS target_version,v.active_index_revision FROM ai_knowledge_document d JOIN ai_knowledge_document_version v ON v.document_id=d.id AND v.tenant_id=d.tenant_id WHERE v.id=? AND v.tenant_id=? AND d.status='ACTIVE' FOR UPDATE", version, tenant);
                int activeRevision = document.get("active_index_revision") == null ? 0 : Integer.parseInt(document.get("active_index_revision").toString());
                Integer latestRevision = jdbc.queryForObject("SELECT COALESCE(MAX(CAST(index_revision AS UNSIGNED)),0) FROM ai_knowledge_ingestion_job WHERE tenant_id=? AND document_version_id=?", Integer.class, tenant, version);
                if (Objects.equals(document.get("version"), document.get("target_version")) && revision >= activeRevision && revision == latestRevision) {
                    ingestion.activate(version, revision, tenant);
                    jdbc.update("UPDATE ai_knowledge_document_version SET active_index_revision=?,index_status='INDEXED',error_message=NULL WHERE id=? AND tenant_id=?", String.valueOf(revision), version, tenant);
                }
                jdbc.update("UPDATE ai_knowledge_ingestion_job SET status='SUCCEEDED',progress=100,finish_time=CURRENT_TIMESTAMP(3),lease_expire_time=NULL,error_message=NULL WHERE id=? AND worker_token=?", id, token);
            });
        } catch (Exception error) {
            String message = com.xqy.sms.ai.infrastructure.security.AiSafetyPolicy.redact(error.getMessage() == null ? "入库失败" : error.getMessage());
            if (message.length() > 1000) message = message.substring(0, 1000);
            cancelIfOwned(id, token);
            if (jdbc.update("UPDATE ai_knowledge_ingestion_job SET status='FAILED',error_message=?,finish_time=CURRENT_TIMESTAMP(3),lease_expire_time=NULL WHERE id=? AND worker_token=? AND status='RUNNING' AND cancel_requested=0", message, id, token) == 1)
                jdbc.update("UPDATE ai_knowledge_document_version v SET index_status='FAILED',error_message=? WHERE id=? AND tenant_id=? AND NOT EXISTS (SELECT 1 FROM ai_knowledge_ingestion_job j WHERE j.document_version_id=v.id AND j.tenant_id=v.tenant_id AND CAST(j.index_revision AS UNSIGNED)>?)", message, version, tenant, revision);
            log.warn("Knowledge ingestion failed for job {}: {}", id, error.getClass().getSimpleName());
        }
    }

    public void activateIndex(Long version, int revision, InternalCallContext caller) {
        authorization.authorize("knowledge:index:activate", caller);
        if (revision <= 0) throw new IllegalArgumentException("无效索引版本");
        transaction.executeWithoutResult(status -> {
            List<Map<String, Object>> documents = jdbc.queryForList("SELECT d.id,v.version FROM ai_knowledge_document d JOIN ai_knowledge_document_version v ON v.document_id=d.id AND v.tenant_id=d.tenant_id WHERE v.id=? AND v.tenant_id=? AND d.status='ACTIVE' FOR UPDATE", version, caller.tenantId());
            if (documents.isEmpty()) throw new ManagementNotFoundException();
            Integer succeeded = jdbc.queryForObject("SELECT COUNT(*) FROM ai_knowledge_ingestion_job WHERE tenant_id=? AND document_version_id=? AND index_revision=? AND status='SUCCEEDED'", Integer.class, caller.tenantId(), version, String.valueOf(revision));
            if (succeeded == null || succeeded == 0) throw new ManagementConflictException("只能启用已成功入库的索引版本");
            ingestion.activate(version, revision, caller.tenantId());
            jdbc.update("UPDATE ai_knowledge_document_version SET active_index_revision=?,index_status='INDEXED',error_message=NULL WHERE id=? AND tenant_id=?", String.valueOf(revision), version, caller.tenantId());
            jdbc.update("UPDATE ai_knowledge_document SET version=? WHERE id=? AND tenant_id=?", documents.getFirst().get("version"), documents.getFirst().get("id"), caller.tenantId());
        });
    }

    private boolean renew(long id, String token) {
        return jdbc.update("UPDATE ai_knowledge_ingestion_job SET lease_expire_time=DATE_ADD(CURRENT_TIMESTAMP(3),INTERVAL 120 SECOND) WHERE id=? AND worker_token=? AND status='RUNNING' AND cancel_requested=0", id, token) == 1;
    }
    private void cancelIfOwned(long id, String token) {
        jdbc.update("UPDATE ai_knowledge_ingestion_job SET status='CANCELLED',finish_time=CURRENT_TIMESTAMP(3),lease_expire_time=NULL WHERE id=? AND worker_token=? AND status='RUNNING' AND cancel_requested=1", id, token);
    }
}
