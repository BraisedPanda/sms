package com.xqy.sms.ai.service.plan;

import cn.hutool.json.JSONUtil;
import com.xqy.sms.ai.model.AiRequest;
import com.xqy.sms.ai.model.AiTask;
import com.xqy.sms.ai.model.AiToolDefinitionProvider;
import com.xqy.sms.ai.model.QueryCriteria;
import com.xqy.sms.ai.service.chat.assistant.AiChatAssistant;
import com.xqy.sms.ai.service.plan.assistant.AiPlanAssistant;
import com.xqy.sms.common.entity.AiToolDefinition;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PreDestroy;
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

    private final AiChatAssistant aiChatAssistant;
    private final AiPlanAssistant aiPlanAssistant;
    private final ThreadPoolExecutor planningExecutor;
    private final StringRedisTemplate stringRedisTemplate;
    private final AiToolDefinitionProvider toolDefinitionProvider;

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 10;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final String CHAT_PREFIX = "chat_";
    private static final String BUSINESS_PREFIX = "business_";

    /** Kept for direct callers/tests that do not configure a tool registry. */
    public AiPlanService(OpenAiChatModel openAiChatModel,
                         StringRedisTemplate stringRedisTemplate,
                         StreamingChatModel streamingChatModel) {
        this(openAiChatModel, stringRedisTemplate, streamingChatModel, (AiToolDefinitionProvider) null);
    }

    /** The registry is optional so planning still supports a chat-only fallback. */
    @Autowired
    public AiPlanService(OpenAiChatModel openAiChatModel,
                         StringRedisTemplate stringRedisTemplate,
                         StreamingChatModel streamingChatModel,
                         ObjectProvider<AiToolDefinitionProvider> providers) {
        this(openAiChatModel, stringRedisTemplate, streamingChatModel,
                providers == null ? null : providers.getIfAvailable());
    }

    /** Constructor useful for embedding the planner with an explicit registry. */
    public AiPlanService(OpenAiChatModel openAiChatModel,
                         StringRedisTemplate stringRedisTemplate,
                         StreamingChatModel streamingChatModel,
                         AiToolDefinitionProvider toolDefinitionProvider) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.toolDefinitionProvider = toolDefinitionProvider;
        this.aiChatAssistant = AiServices.builder(AiChatAssistant.class)
                .chatModel(openAiChatModel)
                .streamingChatModel(streamingChatModel)
                .build();
        this.aiPlanAssistant = AiServices.builder(AiPlanAssistant.class)
                .chatModel(openAiChatModel)
                .build();
        this.planningExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
    }

    @PreDestroy
    public void preDestroy() {
        planningExecutor.shutdown();
    }

    public String sampleChat(String question) {
        return aiChatAssistant.sampleChat(question);
    }

    public SseEmitter chat(AiRequest aiRequest) {
        SseEmitter emitter = createNewSseEmitter();
        sendEvent(emitter, "start", "Chat started");
        try {
            planningExecutor.execute(() -> startChat(emitter, aiRequest));
        } catch (Exception e) {
            sendEvent(emitter, "error", e.getMessage());
            emitter.completeWithError(e);
        }
        return emitter;
    }

    public void startChat(SseEmitter emitter, AiRequest aiRequest) {
        String businessId = buildBusinessId(aiRequest);
        // Reserved for the conversation assistant when task execution is wired in.
        String chatMemoryId = buildMemoryId(CHAT_PREFIX, aiRequest);
        String question = aiRequest.getQuestion();
        String businessContext = stringRedisTemplate.opsForValue().get(businessId);
        List<AiTask> aiTaskList = planTasks(question, businessContext);
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

    /** Return registered definitions, with a useful student query fallback. */
    private List<AiToolDefinition> getAllToolDefinitions() {
        if (toolDefinitionProvider != null) {
            List<AiToolDefinition> definitions = toolDefinitionProvider.getDefinitions();
            if (definitions != null && !definitions.isEmpty()) {
                return definitions.stream().filter(Objects::nonNull).toList();
            }
        }
        AiToolDefinition studentQuery = new AiToolDefinition();
        studentQuery.setDomain("student");
        studentQuery.setToolName("query_student");
        studentQuery.setDescription("按学生姓名、年级、班级、性别等条件查询学生信息");
        studentQuery.setArgumentSpecification("QueryCriteria: name(string), grade(string), className(string), gender(string), studentNo(string), limit(int)");
        studentQuery.setKeywords("学生,查询,年级,班级,姓名,姓氏,性别");
        studentQuery.setEnable(true);
        return List.of(studentQuery);
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
        return new SseEmitter(120L);
    }

    public void sendEvent(SseEmitter emitter, String status, Object data) {
        try {
            emitter.send(SseEmitter.event().name(status).data(data));
        } catch (Exception exception) {
            emitter.completeWithError(exception);
        }
    }

    public String buildBusinessId(AiRequest aiRequest) {
        return buildMemoryId(BUSINESS_PREFIX, aiRequest);
    }

    public String buildMemoryId(String prefix, AiRequest aiRequest) {
        return prefix + aiRequest.getUserId() + "_" + aiRequest.getSessionId();
    }
}
