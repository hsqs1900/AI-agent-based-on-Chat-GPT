package cn.bugstack.ai.test.spring.ai;
import org.junit.Test; // 确保导入JUnit注解
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import cn.bugstack.ai.test.spring.ai.advisors.RagAnswerAdvisor;
import com.alibaba.fastjson.JSON;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * 结合于拼团项目，使用了 ELK 做的结合使用。
 * 如果没有学习拼团项目，可以独立部署ELK验证；https://bugstack.cn/md/road-map/elk.html
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AiAgentMCPESTest {

    private ChatModel chatModel;

    private ChatClient chatClient;

    @Resource
    private PgVectorStore vectorStore;

    public static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";
    public static final String CHAT_MEMORY_RETRIEVE_SIZE_KEY = "chat_memory_response_size";

    @Before
    public void init() {

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("https://apis.itedus.cn")
                .apiKey("sk-qfxpzhJb6UTCdIzf2d7fE9E52e4c497aB9CfA550128f07D0")
                .completionsPath("v1/chat/completions")
                .embeddingsPath("v1/embeddings")
                .build();

        chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4.1-mini")
                        .toolCallbacks(new SyncMcpToolCallbackProvider(stdioMcpClientElasticsearch()).getToolCallbacks())
                        .build())
                .build();
    }

    @Test
    public void test_chat_model_call() {
        Prompt prompt = Prompt.builder()
                .messages(new UserMessage(
                        """
                                我需要查询限流用户，请先根据MCP能力，编写一套提示词。之后我会根据提示词步骤执行。
                                """))
                .build();

        ChatResponse chatResponse = chatModel.call(prompt);

        log.info("测试结果(call):{}", JSON.toJSONString(chatResponse));
    }

    @Test
    public void test_chat_model_call_es() {
        Prompt prompt = Prompt.builder()
                .messages(new UserMessage(
                        """ 
                                查询xfg01日志，DSL 语句；
                                {
                                  `index`: `[从list_indices()获取的实际索引名]`,
                                  `queryBody`: {
                                    `size`: 10,
                                    `sort`: [
                                      {
                                        `@timestamp`: {
                                          `order`: `desc`
                                        }
                                      }
                                    ],
                                    `query`: {
                                      `match`: {
                                        `message`: `xfg01`
                                      }
                                    }
                                  }
                                }
                                """))
                .build();

        ChatResponse chatResponse = chatModel.call(prompt);

        log.info("测试结果(call):{}", JSON.toJSONString(chatResponse));
    }

    /**
     * https://sai.baidu.com/server/Elasticsearch%2520MCP%2520Server/awesimon?id=02d6b7e9091355b91fed045b9c80dede
     * https://github.com/elastic/mcp-server-elasticsearch
     */
    public McpSyncClient stdioMcpClientElasticsearch() {

        Map<String, String> env = new HashMap<>();
        env.put("ES_URL","http://127.0.0.1:9200");
        env.put("ES_API_KEY","none");

        var stdioParams = ServerParameters.builder("F:\\node\\node_JS\\npx.cmd")
                .args("-y", "@elastic/mcp-server-elasticsearch")
                .env(env)
                .build();

        var mcpClient = McpClient.sync(new StdioClientTransport(stdioParams))
                .requestTimeout(Duration.ofSeconds(100)).build();

        var init = mcpClient.initialize();

        System.out.println("Stdio MCP Initialized: " + init);

        return mcpClient;

    }

}
