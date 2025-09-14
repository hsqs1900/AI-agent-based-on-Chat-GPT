package cn.bugstack.ai.test.spring.ai;

import com.alibaba.fastjson.JSON;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 结合于拼团项目，使用了 ELK 做的结合使用。
 * 如果没有学习拼团项目，可以独立部署ELK验证；https://bugstack.cn/md/road-map/elk.html
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class DynamicRateLimitQueryTest {

    private ChatModel chatModel;
    private final List<String> executionLog = new ArrayList<>();
    private final Map<String, Object> analysisContext = new HashMap<>();

    @Before
    public void init() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("https://apis.itedus.cn")
                .apiKey("sk-k6dvxKUVTtjuRquKF6B1E15574794cF9B6006b9cA61bBaD2")
                .completionsPath("v1/chat/completions")
                .embeddingsPath("v1/embeddings")
                .build();

        chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4o-mini")
                        .toolCallbacks(new SyncMcpToolCallbackProvider(stdioMcpClientElasticsearch2()).getToolCallbacks())
                        .build())
                .build();
    }

    public McpSyncClient stdioMcpClientElasticsearch() {
        Map<String, String> env = new HashMap<>();
        env.put("ES_URL", "http://127.0.0.1:9200");
        env.put("ES_API_KEY", "none");

        var stdioParams = ServerParameters.builder("npx")
                .args("-y", "@elastic/mcp-server-elasticsearch")
                .env(env)
                .build();

        var mcpClient = McpClient.sync(new StdioClientTransport(stdioParams))
                .requestTimeout(Duration.ofSeconds(100)).build();

        var init = mcpClient.initialize();
        System.out.println("Stdio MCP Initialized: " + init);
        return mcpClient;
    }

    public McpSyncClient stdioMcpClientElasticsearch2() {
        Map<String, String> env = new HashMap<>();
        env.put("ES_HOST", "http://117.72.214.115:9200");
        env.put("ES_API_KEY", "none");
        // 禁用OpenTelemetry以避免日志干扰JSON-RPC通信
//        env.put("OTEL_SDK_DISABLED", "true");
//        env.put("NODE_OPTIONS", "--no-warnings");

        var stdioParams = ServerParameters.builder("npx")
                .args("-y", "@awesome-ai/elasticsearch-mcp")
                .env(env)
                .build();

        var mcpClient = McpClient.sync(new StdioClientTransport(stdioParams))
                .requestTimeout(Duration.ofSeconds(100)).build();

        var init = mcpClient.initialize();

        System.out.println("Stdio MCP Initialized: " + init);

        return mcpClient;

    }

    /**
     * 动态执行限流用户查询 - 主入口
     */
    @Test
    public void queryRateLimitedUsersDynamic() {
        String userQuery = "查询哪个用户被限流了";
        
        // 创建进度监听器
        Consumer<String> progressListener = progress -> {
            System.out.println("🔄 " + progress);
            log.info("执行进度: {}", progress);
        };
        
        try {
            String result = executeAnalysisWorkflow(userQuery, progressListener);
            System.out.println("\n" + "=".repeat(80));
            System.out.println("📋 最终分析结果:");
            System.out.println("=".repeat(80));
            System.out.println(result);
            
            // 输出执行日志
            System.out.println("\n" + "=".repeat(80));
            System.out.println("📝 执行步骤日志:");
            System.out.println("=".repeat(80));
            executionLog.forEach(System.out::println);
            
        } catch (Exception e) {
            log.error("查询过程中发生错误", e);
            System.err.println("❌ 查询失败: " + e.getMessage());
        }
    }

    /**
     * 执行动态分析工作流
     */
    public String executeAnalysisWorkflow(String userQuery, Consumer<String> progressListener) {
        // 初始化分析上下文
        analysisContext.clear();
        executionLog.clear();
        analysisContext.put("userQuery", userQuery);
        analysisContext.put("currentStep", 0);
        analysisContext.put("maxSteps", 10); // 最大执行步数防止死循环
        
        progressListener.accept("开始执行限流用户查询分析...");
        
        // 构建智能决策系统提示词
        String systemPrompt = buildIntelligentSystemPrompt();
        String currentPrompt = String.format("%s\n\n用户查询: %s", systemPrompt, userQuery);
        
        StringBuilder fullResult = new StringBuilder();
        
        while ((Integer) analysisContext.get("currentStep") < (Integer) analysisContext.get("maxSteps")) {
            int currentStep = (Integer) analysisContext.get("currentStep") + 1;
            analysisContext.put("currentStep", currentStep);
            
            progressListener.accept(String.format("执行第 %d 步分析...", currentStep));
            
            // 执行当前步骤
            String stepResult = executeIntelligentStep(currentPrompt, progressListener);
            fullResult.append(stepResult).append("\n\n");
            
            // 分析执行结果，决定下一步
            NextStepDecision decision = analyzeStepResult(stepResult, progressListener);
            
            if (decision.isComplete()) {
                progressListener.accept("✅ 分析完成！");
                break;
            } else if (decision.shouldContinue()) {
                progressListener.accept("➡️  " + decision.getNextAction());
                currentPrompt = decision.getNextPrompt();
                analysisContext.putAll(decision.getUpdatedContext());
            } else {
                progressListener.accept("⚠️  分析遇到问题，尝试调整策略...");
                currentPrompt = buildRecoveryPrompt(stepResult);
            }
            
            // 添加短暂延迟，避免API调用过于频繁
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return fullResult.toString();
    }

    /**
     * 构建智能决策系统提示词 - 根据MCP Elasticsearch服务实际能力优化。可动态根据mcp能力，ai编写可用提示词。
     */
    private String buildIntelligentSystemPrompt() {
        return """
        你是一个智能的日志分析专家，具备自主决策和动态执行能力。
        你可以操作Elasticsearch来查找限流用户信息。
        
        **你的核心能力和正确用法:**
        
        1. 查询所有索引: list_indices()
           - 无需参数
           - 返回所有可用的Elasticsearch索引列表
        
        2. 获取索引字段映射: get_mappings(index)
           - 参数: index (字符串) - 索引名称
           - 返回该索引的字段结构和类型信息
        
        3. 执行搜索查询: search(index, queryBody)
           - 参数1: index (字符串) - 要搜索的索引名称
           - 参数2: queryBody (JSON对象) - 完整的Elasticsearch查询DSL
           
        **搜索查询示例格式:**
        ```json
        {
          "query": {
            "bool": {
              "should": [
                {"match": {"message": "限流"}},
                {"match": {"message": "rate limit"}},
                {"match": {"message": "throttle"}}
              ]
            }
          },
          "size": 20,
          "sort": [{"@timestamp": {"order": "desc"}}]
        }
        ```
        
        或者简单的全文搜索：
        ```json
        {
          "query": {
            "query_string": {
              "query": "限流 OR rate_limit OR throttle",
              "default_field": "message"
            }
          },
          "size": 50
        }
        ```
        
        **智能执行规则:**
        每次回复必须包含两个部分：
        
        [ANALYSIS] - 当前步骤的分析结果和思考过程
        [NEXT_STEP] - 下一步执行计划，格式如下：
        - ACTION: [具体要执行的动作]
        - REASON: [执行原因]
        - COMPLETE: [是否完成分析，true/false]
        
        **执行策略:**
        1. 首次执行: 调用 list_indices() 探索可用数据源
        2. 选择相关索引: 重点关注包含 log、springboot、application 等关键词的索引
        3. 分析索引结构: 调用 get_mappings() 了解字段结构，特别关注消息字段
        4. 构建搜索查询: 使用合适的Elasticsearch DSL查询限流相关信息
        5. 分析搜索结果: 提取用户信息、限流原因、时间等关键数据
        6. 如果结果不理想，调整搜索策略（修改关键词、扩大搜索范围等）
        
        **限流检测关键词:** 
        - 中文: 限流、超过限制、访问频率过高、黑名单、被封禁
        - 英文: rate limit、throttle、blocked、exceeded、frequency limit
        - 日志级别: ERROR、WARN 通常包含限流信息
        
        **重要提醒:**
        - search() 函数的 queryBody 参数必须是完整的JSON对象，不能为空
        - 优先搜索最近的日志数据，使用时间排序
        - 如果某个搜索没有结果，尝试更宽泛的搜索条件
        - 提取具体的用户标识（用户ID、用户名、IP地址等）
        
        现在开始智能分析，每一步都要详细说明你的思考过程和下一步计划。记住严格按照MCP接口规范调用工具。
        """;
    }

    /**
     * 执行智能步骤
     */
    private String executeIntelligentStep(String prompt, Consumer<String> progressListener) {
        try {
            Prompt chatPrompt = Prompt.builder()
                    .messages(new UserMessage(prompt))
                    .build();
            
            ChatResponse response = chatModel.call(chatPrompt);
            String result = response.getResult().getOutput().getText();
            
            // 记录执行日志
            String logEntry = String.format("步骤 %d: %s", 
                (Integer) analysisContext.get("currentStep"),
                extractAnalysisFromResult(result));
            executionLog.add(logEntry);
            
            return result;
            
        } catch (Exception e) {
            String errorMsg = "执行步骤时发生错误: " + e.getMessage();
            progressListener.accept("❌ " + errorMsg);
            executionLog.add(errorMsg);
            return errorMsg;
        }
    }

    /**
     * 分析步骤结果，决定下一步行动
     */
    private NextStepDecision analyzeStepResult(String stepResult, Consumer<String> progressListener) {
        NextStepDecision decision = new NextStepDecision();
        
        try {
            // 解析AI的回复
            String nextStepSection = extractNextStepSection(stepResult);
            
            if (nextStepSection.contains("COMPLETE: true") || 
                stepResult.contains("分析完成") || 
                stepResult.contains("查询结果") && stepResult.contains("用户")) {
                
                decision.setComplete(true);
                decision.setNextAction("分析完成，已找到限流用户信息");
                
            } else {
                decision.setComplete(false);
                decision.setShouldContinue(true);
                
                // 提取下一步行动
                String action = extractFieldValue(nextStepSection, "ACTION");
                String reason = extractFieldValue(nextStepSection, "REASON");
                
                decision.setNextAction(action.isEmpty() ? "继续分析" : action);
                
                // 构建下一步的提示词
                String nextPrompt = buildNextPrompt(stepResult, action, reason);
                decision.setNextPrompt(nextPrompt);
            }
            
        } catch (Exception e) {
            progressListener.accept("⚠️  解析步骤结果时出错，使用默认策略");
            decision.setComplete(false);
            decision.setShouldContinue(true);
            decision.setNextAction("尝试其他搜索策略");
            decision.setNextPrompt(buildRecoveryPrompt(stepResult));
        }
        
        return decision;
    }

    /**
     * 提取分析部分内容
     */
    private String extractAnalysisFromResult(String result) {
        Pattern pattern = Pattern.compile("\\[ANALYSIS\\](.*?)(?=\\[NEXT_STEP\\]|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(result);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return result.length() > 200 ? result.substring(0, 200) + "..." : result;
    }

    /**
     * 提取下一步执行部分
     */
    private String extractNextStepSection(String result) {
        Pattern pattern = Pattern.compile("\\[NEXT_STEP\\](.*?)$", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(result);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    /**
     * 提取字段值
     */
    private String extractFieldValue(String text, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + ":\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    /**
     * 构建下一步提示词
     */
    private String buildNextPrompt(String previousResult, String action, String reason) {
        return String.format("""
            基于上一步的分析结果，请继续执行：
            
            上一步结果：
            %s
            
            下一步行动：%s
            执行原因：%s
            
            请执行相应的操作，并继续按照 [ANALYSIS] 和 [NEXT_STEP] 的格式回复。
            """, previousResult, action, reason);
    }

    /**
     * 构建恢复提示词（当分析出错时）
     */
    private String buildRecoveryPrompt(String previousResult) {
        return String.format("""
            之前的分析可能遇到了一些问题，请重新评估当前情况：
            
            之前的结果：
            %s
            
            请重新分析当前状态，并决定最佳的下一步行动。
            如果已经有足够的信息，请总结限流用户的情况。
            如果还需要更多数据，请选择最有效的查询策略。
            
            继续按照 [ANALYSIS] 和 [NEXT_STEP] 的格式回复。
            """, previousResult);
    }

    /**
     * 下一步决策类
     */
    public static class NextStepDecision {
        private boolean complete = false;
        private boolean shouldContinue = true;
        private String nextAction = "";
        private String nextPrompt = "";
        private Map<String, Object> updatedContext = new HashMap<>();

        // Getters and Setters
        public boolean isComplete() { return complete; }
        public void setComplete(boolean complete) { this.complete = complete; }
        
        public boolean shouldContinue() { return shouldContinue; }
        public void setShouldContinue(boolean shouldContinue) { this.shouldContinue = shouldContinue; }
        
        public String getNextAction() { return nextAction; }
        public void setNextAction(String nextAction) { this.nextAction = nextAction; }
        
        public String getNextPrompt() { return nextPrompt; }
        public void setNextPrompt(String nextPrompt) { this.nextPrompt = nextPrompt; }
        
        public Map<String, Object> getUpdatedContext() { return updatedContext; }
        public void setUpdatedContext(Map<String, Object> updatedContext) { this.updatedContext = updatedContext; }
    }

    /**
     * 手动测试分步执行（用于调试）
     */
    @Test
    public void testStepByStepExecution() {
        System.out.println("🧪 开始分步测试...");
        
        // 测试步骤1：探索索引
        testSingleStep("探索可用的Elasticsearch索引", 
            "请调用list_indices()查看所有可用的索引，并按照格式回复");
        
        // 测试步骤2：获取映射
        testSingleStep("获取日志索引的字段映射", 
            "请对springboot-logstash相关的索引调用get_mappings()，并按照格式回复");
        
        // 测试步骤3：搜索限流日志
        testSingleStep("搜索限流相关日志", 
            "请搜索包含'限流'关键词的日志，并按照格式回复");
    }

    private void testSingleStep(String stepName, String instruction) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("🔍 测试: " + stepName);
        System.out.println("=".repeat(50));
        
        String prompt = buildIntelligentSystemPrompt() + "\n\n" + instruction;
        String result = executeIntelligentStep(prompt, progress -> System.out.println("📋 " + progress));
        
        System.out.println("📤 结果:");
        System.out.println(result);
        
        NextStepDecision decision = analyzeStepResult(result, progress -> {});
        System.out.println("\n📋 决策: " + decision.getNextAction());
        System.out.println("🏁 完成: " + decision.isComplete());
    }
}
