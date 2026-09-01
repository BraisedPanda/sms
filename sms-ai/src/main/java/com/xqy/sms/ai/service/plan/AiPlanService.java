package com.xqy.sms.ai.service.plan;

import cn.hutool.json.JSONUtil;
import com.xqy.sms.ai.model.*;
import com.xqy.sms.ai.service.chat.AiChatService;
import com.xqy.sms.ai.service.student.StudentBusinessService;
import com.xqy.sms.ai.service.plan.assistant.AiPlanAssistant;
import com.xqy.sms.common.entity.AiToolDefinition;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class AiPlanService {

    private final AiPlanAssistant aiPlanAssistant;
    private final ThreadPoolExecutor planningExecutor;
    private final ThreadPoolExecutor taskExecutor;
    private final StringRedisTemplate stringRedisTemplate;
    private final AiToolDefinitionProvider toolDefinitionProvider;
    private final AiChatService aiChatService;
    private final StudentBusinessService studentBusinessService;

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 10;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final String CHAT_PREFIX = "chat_";
    private static final String BUSINESS_PREFIX = "business_";



    /** Constructor useful for embedding the planner with an explicit registry. */
    public AiPlanService(OpenAiChatModel openAiChatModel,
                         StringRedisTemplate stringRedisTemplate,
                         StreamingChatModel streamingChatModel,
                         AiToolDefinitionProvider toolDefinitionProvider,
                         AiChatService aiChatService,
                         StudentBusinessService studentBusinessService
                         ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.toolDefinitionProvider = toolDefinitionProvider;
        this.aiChatService = aiChatService;
        this.studentBusinessService = studentBusinessService;
        this.aiPlanAssistant = AiServices.builder(AiPlanAssistant.class)
                .chatModel(openAiChatModel)
                .build();
        this.planningExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());

        this.taskExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
    }

    @PreDestroy
    public void preDestroy() {
        planningExecutor.shutdown();
        taskExecutor.shutdown();
    }

    public String sampleChat(String question) {
        return aiChatService.sampleChat(question);
    }

    public SseEmitter chat(AiTaskRequest aiTaskRequest) {
        SseEmitter emitter = createNewSseEmitter();
        sendEvent(emitter, "start", "Chat started");
        try {
            planningExecutor.execute(() -> startChat(emitter, aiTaskRequest));
        } catch (Exception e) {
            sendEvent(emitter, "error", e.getMessage());
            emitter.completeWithError(e);
        }
        return emitter;
    }

    public void startChat(SseEmitter emitter, AiTaskRequest aiTaskRequest) {
        try {
            String businessId = buildBusinessId(aiTaskRequest);
            String chatMemoryId = buildMemoryId(CHAT_PREFIX, aiTaskRequest);
            String question = aiTaskRequest.getQuestion();
            String businessContext = stringRedisTemplate.opsForValue().get(businessId);
            List<AiTask> aiTaskList = planTasks(question, businessContext);
            if (CollectionUtils.isEmpty(aiTaskList)) {
                sendEvent(emitter, "error", "AI 未能生成可执行任务");
                emitter.complete();
            } else if (isChatTask(aiTaskList)) {
                aiChatService.streamChat(emitter, question, chatMemoryId);
            } else {
                executeTasks(emitter, aiTaskList, chatMemoryId, businessId, question);
            }
        } catch (Exception error) {
            sendEvent(emitter, "error", error.getMessage());
            emitter.completeWithError(error);
        }
    }

    private void executeTasks(SseEmitter emitter, List<AiTask> aiTaskList, String chatMemoryId,
                              String businessId, String question) {
        try {
            List<java.util.concurrent.Future<AiTaskResult>> futures = aiTaskList.stream()
                    .map(task -> taskExecutor.submit(() -> executeTask(task)))
                    .toList();
            List<AiTaskResult> results = new ArrayList<>(futures.size());
            for (java.util.concurrent.Future<AiTaskResult> future : futures) {
                results.add(future.get());
            }
            String resultJson = JSONUtil.toJsonStr(results);
            stringRedisTemplate.opsForValue().set(businessId, resultJson, 30, TimeUnit.MINUTES);
            aiChatService.answer(emitter,
                    "用户问题：\n" + question
                            + "\n\n业务查询结果（只能依据此结果回答，不要编造）：\n" + resultJson,
                    chatMemoryId);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            sendEvent(emitter, "error", "AI 任务执行被中断");
            emitter.completeWithError(interrupted);
        } catch (Exception error) {
            sendEvent(emitter, "error", error.getMessage());
            emitter.completeWithError(error);
        }

    }

    private AiTaskResult executeTask(AiTask task) {
        if (task == null || (task.getMissingArgs() != null && !task.getMissingArgs().isEmpty())) {
            throw new IllegalArgumentException("任务缺少必要参数");
        }
        if ("student".equalsIgnoreCase(task.getDomain())
                && "query_student".equalsIgnoreCase(task.getToolName())) {
            return studentBusinessService.queryStudent(task);
        }
        throw new IllegalArgumentException("未注册的 AI 工具: " + task.getDomain() + "/" + task.getToolName());
    }

    private boolean isChatTask(List<AiTask> aiTaskList) {
        boolean flag = true;
        for (AiTask aiTask : aiTaskList) {
            if (!"chat".equals(aiTask.getDomain())) {
                flag = false;
                break;
            }
        }
        return flag;
    }

    /** Plan executable tasks from the user's question and the current context. */
    public List<AiTask> planTasks(String question, String businessContext) {
        if (question == null || question.isBlank()) {
            return Collections.emptyList();
        }
        List<AiToolDefinition> definitions = getAllToolDefinitions();
        String prompt = buildPlanPrompt(question, businessContext, definitions);
        return parseTaskList(aiPlanAssistant.plan(prompt), definitions);
    }

    /** Compatibility overload; planning calls should include the question. */
    public String buildPlanPrompt(String businessContext, List<AiToolDefinition> definitions) {
        return buildPlanPrompt(null, businessContext, definitions);
    }

    /** Build a strict JSON-only prompt with an explicit QueryCriteria schema. */
    public String buildPlanPrompt(String question, String businessContext,
                                  List<AiToolDefinition> definitions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个 AI 规划助手，负责把用户问题拆分成一个或多个可执行任务。\n")
                .append("只能输出 JSON 数组，不能输出 Markdown、代码块、解释或额外文字。\n")
                .append("每个任务必须符合：{\"domain\":\"chat 或工具定义中的 domain\",\"toolName\":\"工具名或 null\",\n")
                .append("\"reason\":\"执行理由\",\"query\":{\"limit\":100,\"filter\":{}},\"missingArgs\":[]}。\n")
                .append("domain=chat 时 toolName 必须为 null，query 可为 null；domain 不是 chat 时必须从工具定义中选择 toolName。\n")
                .append("QueryCriteria 用于转换为 MyBatis-Plus QueryWrapper：\n")
                .append("filter 叶子节点使用 field、operator、value；逻辑节点使用 and、or、not。\n")
                .append("operator 只能使用 EQ、NE、LIKE、NOT_LIKE、GT、GE、LT、LE、IN、NOT_IN、BETWEEN、IS_NULL、IS_NOT_NULL；IN/NOT_IN/BETWEEN 使用 values。\n")
                .append("提取问题中的所有明确条件（例如年级、班级、姓氏、性别、时间和数量），相互独立的条件放入同一个 and 数组；不确定的参数放入 missingArgs。\n")
                .append("field 必须使用工具参数说明中的实体属性或数据库字段名；未指定数量时 limit=100，没有条件时 filter=null。\n\n")
                .append("解析示例：‘查询一年级1班所有姓王的女生信息’应生成 student/query_student，"
                        + "并在 query.filter.and 中放入 grade EQ ‘一年级’、className EQ ‘1班’、name LIKE ‘王’、gender EQ ‘女’ 四个条件。\n\n")
                .append("用户问题：\n---\n")
                .append(question == null ? "(未提供)" : question)
                .append("\n---\n业务上下文：\n---\n")
                .append(businessContext == null || businessContext.isBlank() ? "(无)" : businessContext)
                .append("\n---\n可用工具定义：\n");

        if (definitions == null || definitions.isEmpty()) {
            prompt.append("(无；仅能生成 chat 任务)\n");
        } else {
            for (AiToolDefinition definition : definitions) {
                if (definition == null) {
                    continue;
                }
                prompt.append("- domain: ").append(valueOrEmpty(definition.getDomain()))
                        .append(", toolName: ").append(valueOrEmpty(definition.getToolName()))
                        .append("\n  描述: ").append(valueOrEmpty(definition.getDescription()))
                        .append("\n  参数说明: ").append(valueOrEmpty(definition.getArgumentSpecification()))
                        .append("\n  关键词: ").append(valueOrEmpty(definition.getKeywords())).append("\n");
            }
        }
        return prompt.toString();
    }

    /** Return the definitions supplied by the database-backed provider. */
    private List<AiToolDefinition> getAllToolDefinitions() {
        if (toolDefinitionProvider == null) {
            return Collections.emptyList();
        }
        List<AiToolDefinition> definitions = toolDefinitionProvider.getDefinitions();
        return definitions == null
                ? Collections.emptyList()
                : definitions.stream().filter(Objects::nonNull).toList();
    }

    private List<AiTask> parseTaskList(String result, List<AiToolDefinition> definitions) {
        if (result == null || result.isBlank()) {
            return Collections.emptyList();
        }
        String json = result.trim();
        if (json.startsWith("```") && json.endsWith("```")) {
            int firstLineEnd = json.indexOf('\n');
            json = firstLineEnd >= 0
                    ? json.substring(firstLineEnd + 1, json.length() - 3).trim()
                    : json.substring(3, json.length() - 3).trim();
        }
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        try {
            List<AiTask> tasks = JSONUtil.toList(json, AiTask.class);
            if (tasks == null) {
                return Collections.emptyList();
            }
            for (AiTask task : tasks) {
                if (task == null) {
                    continue;
                }
                if (task.getDomain() != null) {
                    task.setDomain(task.getDomain().trim().toLowerCase(Locale.ROOT));
                }
                if (task.getMissingArgs() == null) {
                    task.setMissingArgs(new ArrayList<>());
                }
                if ("chat".equals(task.getDomain())) {
                    // A chat task has no executable domain tool.
                    task.setToolName(null);
                } else if (task.getToolName() == null || task.getToolName().isBlank()) {
                    // Repair the common model omission when a domain has one
                    // unambiguous tool; otherwise make the missing input explicit.
                    String candidate = uniqueToolForDomain(task.getDomain(), definitions);
                    if (candidate != null) {
                        task.setToolName(candidate);
                    } else {
                        task.getMissingArgs().add("toolName");
                    }
                }
                if (!"chat".equals(task.getDomain()) && task.getQuery() == null) {
                    task.setQuery(new QueryCriteria());
                }
            }
            return tasks;
        } catch (Exception exception) {
            throw new IllegalArgumentException("AI planner returned invalid task JSON", exception);
        }
    }

    private static String uniqueToolForDomain(String domain, List<AiToolDefinition> definitions) {
        if (domain == null || definitions == null) {
            return null;
        }
        String candidate = null;
        for (AiToolDefinition definition : definitions) {
            if (definition == null || !domain.equalsIgnoreCase(definition.getDomain())
                    || definition.getToolName() == null || definition.getToolName().isBlank()) {
                continue;
            }
            if (candidate != null && !candidate.equals(definition.getToolName())) {
                return null;
            }
            candidate = definition.getToolName();
        }
        return candidate;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private void sendToken(SseEmitter emitter, String token) {
        try {
            emitter.send(SseEmitter.event().data(token));
        } catch (Exception exception) {
            emitter.completeWithError(exception);
        }
    }

    public SseEmitter createNewSseEmitter() {
        return new SseEmitter(120_000L);
    }

    public void sendEvent(SseEmitter emitter, String status, Object data) {
        try {
            emitter.send(SseEmitter.event().name(status).data(data));
        } catch (Exception exception) {
            emitter.completeWithError(exception);
        }
    }

    public String buildBusinessId(AiTaskRequest aiTaskRequest) {
        return buildMemoryId(BUSINESS_PREFIX, aiTaskRequest);
    }

    public String buildMemoryId(String prefix, AiTaskRequest aiTaskRequest) {
        return prefix + aiTaskRequest.getUserId() + "_" + aiTaskRequest.getSessionId();
    }
}
