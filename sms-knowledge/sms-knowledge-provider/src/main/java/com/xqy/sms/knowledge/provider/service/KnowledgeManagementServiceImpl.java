package com.xqy.sms.knowledge.provider.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xqy.sms.common.dto.ManagementRows;
import com.xqy.sms.common.dto.PageResult;
import com.xqy.sms.common.exception.ManagementConflictException;
import com.xqy.sms.common.exception.ManagementNotFoundException;
import com.xqy.sms.common.security.rpc.InternalCallContext;
import com.xqy.sms.knowledge.api.service.KnowledgeManagementService;
import com.xqy.sms.system.api.service.SystemManagementService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

@Service
@DubboService
public class KnowledgeManagementServiceImpl implements KnowledgeManagementService {
    private static final Map<String, String> TABLES = Map.of("bases", "ai_knowledge_base", "documents", "ai_knowledge_document",
            "versions", "ai_knowledge_document_version", "jobs", "ai_knowledge_ingestion_job", "chunks", "ai_knowledge_document_detail");
    private static final Map<String, String> COLUMNS = Map.of(
            "bases", "id,name,description,embedding_model_alias,chunk_size,chunk_overlap,split_strategy,topk,similarity_threshold,enabled,remark,create_time,update_time",
            "documents", "id,knowledge_base_id,document_no,document_name,source_type,version,category,keywords,status,create_time,update_time",
            "versions", "id,document_id,version,source_version,mime_type,content_hash,chunk_size,chunk_overlap,split_strategy,active_index_revision,index_status,chunk_count,error_message,create_time",
            "jobs", "id,document_version_id,index_revision,job_type,status,progress,retry_count,processed_chunks,cancel_requested,start_time,finish_time,error_message,create_time",
            "chunks", "id,document_id,document_version_id,chunk_no,content,content_type,metadata");
    @DubboReference(check = false) private SystemManagementService authorization;
    private final JdbcTemplate jdbc;
    private final DocumentTextParser parser;
    private final DocumentChunker chunker;
    private final ObjectMapper json;
    private final Path uploadDirectory;

    public KnowledgeManagementServiceImpl(JdbcTemplate jdbc, DocumentTextParser parser, DocumentChunker chunker,
                                          ObjectMapper json, @Value("${sms.knowledge.upload-directory}") String uploadDirectory) {
        this.jdbc = jdbc; this.parser = parser; this.chunker = chunker; this.json = json;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @Override
    public PageResult list(String resource, Map<String, String> filters, int current, int size, InternalCallContext caller) {
        authorize(resource, "read", caller);
        filters = filters == null ? Map.of() : filters;
        List<Object> args = new ArrayList<>(); args.add(caller.tenantId());
        String where = " WHERE tenant_id=?";
        if (resource.equals("documents")) where += " AND status='ACTIVE'";
        Map<String, String> parent = switch (resource) { case "documents" -> Map.of("knowledgeBaseId", "knowledge_base_id");
            case "versions" -> Map.of("documentId", "document_id"); case "jobs" -> Map.of("documentVersionId", "document_version_id");
            case "chunks" -> Map.of("documentVersionId", "document_version_id"); default -> Map.of(); };
        for (var entry : parent.entrySet()) if (filters.get(entry.getKey()) != null && !filters.get(entry.getKey()).isBlank()) {
            where += " AND " + entry.getValue() + "=?"; args.add(ManagementRows.id(filters.get(entry.getKey())));
        }
        String search = ManagementRows.text(filters.get("search"), 128, false);
        if (search != null && !search.isBlank()) {
            String column = switch (resource) { case "bases" -> "name"; case "documents" -> "document_name"; case "versions" -> "version"; case "jobs" -> "status"; default -> "content"; };
            where += " AND " + column + " LIKE ?"; args.add("%" + search + "%");
        }
        current = Math.max(1, current); size = Math.max(1, Math.min(200, size));
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + table(resource) + where, Long.class, args.toArray());
        args.add(size); args.add(((long) current - 1) * size);
        return new PageResult(ManagementRows.publicRows(jdbc.queryForList("SELECT " + COLUMNS.get(resource) + " FROM " + table(resource)
                + where + " ORDER BY id DESC LIMIT ? OFFSET ?", args.toArray())), current, size, total == null ? 0 : total);
    }

    @Override
    @Transactional
    public Map<String, Object> save(String resource, Long id, Map<String, Object> input, InternalCallContext caller) {
        authorize(resource, id == null ? "create" : "update", caller);
        if (input == null || !(resource.equals("bases") || resource.equals("documents"))) throw new IllegalArgumentException("未知写操作");
        if (resource.equals("documents") && id == null) throw new IllegalArgumentException("请通过上传创建文档");
        Map<String, Object> previous = id == null ? Map.of() : owned(resource, id, caller);
        Set<String> fields = resource.equals("bases") ? Set.of("name", "description", "embeddingModelAlias", "chunkSize", "chunkOverlap", "splitStrategy", "topk", "similarityThreshold", "enabled", "remark")
                : Set.of("documentName", "category", "keywords");
        Map<String, Object> values = new LinkedHashMap<>();
        if (id == null) {
            id = IdWorker.getId(); values.put("id", id); values.put("tenant_id", caller.tenantId()); values.put("create_by", caller.userId().toString());
            values.put("chunk_size", 800); values.put("chunk_overlap", 120); values.put("split_strategy", "PARAGRAPH"); values.put("topk", 5); values.put("enabled", true);
        }
        for (var entry : input.entrySet()) {
            if (!fields.contains(entry.getKey())) throw new IllegalArgumentException("不允许写入字段: " + entry.getKey());
            Object value = entry.getValue();
            if (List.of("chunkSize", "chunkOverlap", "topk").contains(entry.getKey())) {
                try { value = Integer.parseInt(String.valueOf(value)); } catch (NumberFormatException error) { throw new IllegalArgumentException("参数必须为整数"); }
            } else if (entry.getKey().equals("enabled")) {
                if (!(value instanceof Boolean)) throw new IllegalArgumentException("启用状态必须为布尔值");
            } else if (entry.getKey().equals("similarityThreshold")) {
                value = value == null ? null : Double.parseDouble(value.toString());
                if (value != null && (!Double.isFinite((Double) value) || (Double) value < 0 || (Double) value > 1)) throw new IllegalArgumentException("相似度须为 0–1");
            } else {
                int maximum = switch (entry.getKey()) { case "name", "embeddingModelAlias", "category" -> 128;
                    case "documentName" -> 255; case "description", "remark" -> 500; case "keywords" -> 4000; default -> 32; };
                value = ManagementRows.text(value, maximum, entry.getKey().equals("name") || entry.getKey().equals("documentName"));
            }
            values.put(snake(entry.getKey()), value);
        }
        if (resource.equals("bases")) {
            if (id != null && previous.isEmpty() && !values.containsKey("name")) throw new IllegalArgumentException("名称不能为空");
            Map<String, Object> effective = new HashMap<>(previous); effective.putAll(values);
            chunker.validate(((Number) effective.get("chunk_size")).intValue(), ((Number) effective.get("chunk_overlap")).intValue(), effective.get("split_strategy").toString());
            int topk = ((Number) effective.get("topk")).intValue(); if (topk < 1 || topk > 100) throw new IllegalArgumentException("检索条数须为 1–100");
        }
        values.put("modify_by", caller.userId().toString());
        try {
            if (previous.isEmpty()) insert(table(resource), values);
            else update(table(resource), id, caller.tenantId(), values);
        } catch (org.springframework.dao.DataIntegrityViolationException error) { throw new ManagementConflictException("名称重复或字段不符合表结构"); }
        return ManagementRows.publicRow(jdbc.queryForMap("SELECT " + COLUMNS.get(resource) + " FROM " + table(resource) + " WHERE id=? AND tenant_id=?", id, caller.tenantId()));
    }

    @Override
    @Transactional
    public void delete(String resource, Long id, InternalCallContext caller) {
        authorize(resource, "delete", caller); owned(resource, id, caller);
        if (resource.equals("bases")) {
            if (count("SELECT COUNT(*) FROM ai_knowledge_document WHERE tenant_id=? AND knowledge_base_id=?", caller.tenantId(), id) > 0)
                throw new ManagementConflictException("知识库仍有文档，可先禁用知识库或删除文档");
            jdbc.update("DELETE FROM ai_knowledge_base WHERE id=? AND tenant_id=?", id, caller.tenantId());
        } else if (resource.equals("documents")) {
            if (count("SELECT COUNT(*) FROM ai_knowledge_ingestion_job j JOIN ai_knowledge_document_version v ON v.id=j.document_version_id AND v.tenant_id=j.tenant_id WHERE v.document_id=? AND j.tenant_id=? AND j.status IN ('PENDING','RUNNING')", id, caller.tenantId()) > 0)
                throw new ManagementConflictException("请先取消或完成此文档的入库任务");
            jdbc.update("UPDATE ai_knowledge_document SET status='DELETED',modify_by=? WHERE id=? AND tenant_id=?", caller.userId().toString(), id, caller.tenantId());
        } else throw new IllegalArgumentException("不能直接删除版本、分块或任务记录");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upload(Long knowledgeBaseId, Long documentId, String filename, byte[] bytes, InternalCallContext caller) {
        authorize("documents", "create", caller);
        Map<String, Object> base = owned("bases", knowledgeBaseId, caller);
        String text = parser.parse(filename, bytes);
        int chunkSize = ((Number) base.get("chunk_size")).intValue(), overlap = ((Number) base.get("chunk_overlap")).intValue();
        String strategy = base.get("split_strategy").toString();
        List<String> chunks = chunker.split(text, chunkSize, overlap, strategy);
        String actor = caller.userId().toString(), tenant = caller.tenantId();
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (documentId == null) {
                documentId = IdWorker.getId();
                jdbc.update("INSERT INTO ai_knowledge_document (id,tenant_id,knowledge_base_id,document_no,document_name,source_type,status,create_by,modify_by) VALUES (?,?,?,?,?,?,'ACTIVE',?,?)",
                        documentId, tenant, knowledgeBaseId, "UPLOAD-" + documentId, filename.replaceAll(".*[\\\\/]", ""), "UPLOAD", actor, actor);
            } else {
                Map<String, Object> document = owned("documents", documentId, caller);
                if (!Objects.equals(((Number) document.get("knowledge_base_id")).longValue(), knowledgeBaseId) || !"ACTIVE".equals(document.get("status")))
                    throw new IllegalArgumentException("文档不属于当前知识库或已删除");
                jdbc.queryForObject("SELECT id FROM ai_knowledge_document WHERE id=? AND tenant_id=? FOR UPDATE", Long.class, documentId, tenant);
                List<Map<String, Object>> same = jdbc.queryForList("SELECT " + COLUMNS.get("versions") + " FROM ai_knowledge_document_version WHERE tenant_id=? AND document_id=? AND content_hash=? AND chunk_size=? AND chunk_overlap=? AND split_strategy=? ORDER BY id DESC LIMIT 1", tenant, documentId, hash, chunkSize, overlap, strategy);
                if (!same.isEmpty()) return ManagementRows.publicRow(same.getFirst());
            }
            long versionId = IdWorker.getId(); String version = "v-" + versionId;
            String storageName = UUID.randomUUID() + "." + parser.extension(filename);
            Files.createDirectories(uploadDirectory);
            Path storage = uploadDirectory.resolve(storageName).normalize();
            if (!storage.startsWith(uploadDirectory)) throw new IllegalArgumentException("无效存储路径");
            Files.write(storage, bytes);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) try { Files.deleteIfExists(storage); } catch (Exception ignored) { }
                }
            });
            String mime = switch (parser.extension(filename)) { case "pdf" -> "application/pdf"; case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"; case "html", "htm" -> "text/html"; default -> "text/plain"; };
            jdbc.update("INSERT INTO ai_knowledge_document_version (id,tenant_id,document_id,version,source_uri,mime_type,content_hash,chunk_size,chunk_overlap,split_strategy,index_status,chunk_count,create_by,modify_by) VALUES (?,?,?,?,?,?,?,?,?,?,'STAGED',?,?,?)",
                    versionId, tenant, documentId, version, "upload://" + storageName, mime, hash, chunkSize, overlap, strategy, chunks.size(), actor, actor);
            for (int i = 0; i < chunks.size(); i++) jdbc.update("INSERT INTO ai_knowledge_document_detail (id,tenant_id,document_id,document_version_id,chunk_no,content,content_type,metadata,create_by,modify_by) VALUES (?,?,?,?,?,?,'text/plain',?,?,?)",
                    IdWorker.getId(), tenant, documentId, versionId, i + 1, chunks.get(i), json.writeValueAsString(Map.of("filename", filename, "strategy", strategy)), actor, actor);
            jdbc.update("UPDATE ai_knowledge_document SET version=?,modify_by=? WHERE id=? AND tenant_id=?", version, actor, documentId, tenant);
            return ManagementRows.publicRow(jdbc.queryForMap("SELECT " + COLUMNS.get("versions") + " FROM ai_knowledge_document_version WHERE id=? AND tenant_id=?", versionId, tenant));
        } catch (RuntimeException error) { throw error; }
        catch (Exception error) { throw new IllegalStateException("文档保存失败", error); }
    }

    @Override
    @Transactional
    public void retryJob(Long id, InternalCallContext caller) {
        authorize("jobs", "retry", caller); Map<String, Object> job = owned("jobs", id, caller);
        if (!List.of("FAILED", "CANCELLED").contains(job.get("status"))) throw new ManagementConflictException("仅失败或取消的任务可以重试");
        if (count("SELECT COUNT(*) FROM ai_knowledge_document_version v JOIN ai_knowledge_document d ON d.id=v.document_id AND d.tenant_id=v.tenant_id JOIN ai_knowledge_base b ON b.id=d.knowledge_base_id AND b.tenant_id=d.tenant_id WHERE v.id=? AND v.tenant_id=? AND d.status='ACTIVE' AND b.enabled=1", job.get("document_version_id"), caller.tenantId()) != 1)
            throw new ManagementConflictException("文档已删除或知识库已禁用");
        jdbc.update("UPDATE ai_knowledge_ingestion_job SET status='PENDING',progress=0,processed_chunks=0,cancel_requested=0,retry_count=retry_count+1,error_message=NULL,finish_time=NULL,worker_token=NULL,lease_expire_time=NULL WHERE id=? AND tenant_id=? AND status IN ('FAILED','CANCELLED')", id, caller.tenantId());
        jdbc.update("UPDATE ai_knowledge_document_version v SET index_status='PENDING',error_message=NULL WHERE id=? AND tenant_id=? AND NOT EXISTS (SELECT 1 FROM ai_knowledge_ingestion_job j WHERE j.document_version_id=v.id AND j.tenant_id=v.tenant_id AND CAST(j.index_revision AS UNSIGNED)>?)", job.get("document_version_id"), caller.tenantId(), Integer.parseInt(job.get("index_revision").toString()));
    }
    @Override
    @Transactional
    public void cancelJob(Long id, InternalCallContext caller) {
        authorize("jobs", "cancel", caller); owned("jobs", id, caller);
        jdbc.update("UPDATE ai_knowledge_ingestion_job SET cancel_requested=1,status=CASE WHEN status='PENDING' THEN 'CANCELLED' ELSE status END,finish_time=CASE WHEN status='PENDING' THEN CURRENT_TIMESTAMP(3) ELSE finish_time END WHERE id=? AND tenant_id=? AND status IN ('PENDING','RUNNING')", id, caller.tenantId());
    }

    private void authorize(String resource, String action, InternalCallContext caller) {
        table(resource);
        String permission = switch (resource) { case "bases" -> "base"; case "jobs" -> "ingestion"; default -> "document"; };
        authorization.authorize("knowledge:" + permission + ":" + action, caller);
    }
    private Map<String, Object> owned(String resource, Long id, InternalCallContext caller) {
        ManagementRows.id(id);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM " + table(resource) + " WHERE id=? AND tenant_id=?", id, caller.tenantId());
        if (rows.isEmpty()) throw new ManagementNotFoundException(); return rows.getFirst();
    }
    private String table(String resource) { String table = TABLES.get(resource); if (table == null) throw new IllegalArgumentException("未知管理对象"); return table; }
    private int count(String sql, Object... args) { Integer value = jdbc.queryForObject(sql, Integer.class, args); return value == null ? 0 : value; }
    private String snake(String key) { return key.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT); }
    private void insert(String table, Map<String, Object> values) { jdbc.update("INSERT INTO " + table + " (" + String.join(",", values.keySet()) + ") VALUES (" + String.join(",", Collections.nCopies(values.size(), "?")) + ")", values.values().toArray()); }
    private void update(String table, Long id, String tenant, Map<String, Object> values) {
        List<Object> args = new ArrayList<>(values.values()); args.add(id); args.add(tenant);
        jdbc.update("UPDATE " + table + " SET " + String.join(",", values.keySet().stream().map(field -> field + "=?").toList()) + " WHERE id=? AND tenant_id=?", args.toArray());
    }
}
