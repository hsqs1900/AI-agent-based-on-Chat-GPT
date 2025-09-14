package cn.bugstack.ai.test.domain;

import cn.bugstack.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.bugstack.ai.domain.agent.model.valobj.enums.AiAgentEnumVO;
import cn.bugstack.ai.domain.agent.service.armory.factory.DefaultArmoryStrategyFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

/**
 * Agent测试类，用于测试AI客户端、模型和API节点的功能
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AgentTest {

    /**
     * 装备策略工厂，用于获取装备策略处理器
     */
    @Resource
    private DefaultArmoryStrategyFactory defaultArmoryStrategyFactory;

    /**
     * Spring应用上下文，用于获取Bean实例
     */
    @Resource
    private ApplicationContext applicationContext;

    /**
     * 测试AI客户端API节点功能
     * 该测试方法演示了如何使用装备策略工厂来处理AI客户端命令，
     * 并从应用上下文中获取OpenAiApi Bean实例。
     *
     * @throws Exception 可能抛出的异常
     */
    @Test
    public void test_aiClientApiNode() throws Exception {
        // 获取装备策略处理器
        StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> armoryStrategyHandler =
                defaultArmoryStrategyFactory.armoryStrategyHandler();

        // 构建装备命令实体并应用策略处理器
        String apply = armoryStrategyHandler.apply(
                ArmoryCommandEntity.builder()
                        .commandType(AiAgentEnumVO.AI_CLIENT.getCode()) // 设置命令类型为AI客户端
                        .commandIdList(Arrays.asList("3001")) // 设置命令ID列表
                        .build(),
                new DefaultArmoryStrategyFactory.DynamicContext()); // 创建动态上下文

        // 从应用上下文中获取OpenAiApi Bean实例
        OpenAiApi openAiApi = (OpenAiApi) applicationContext.getBean(AiAgentEnumVO.AI_CLIENT_API.getBeanName("1001"));

        // 记录测试结果日志
        log.info("测试结果：{}", openAiApi);
    }

    /**
     * 测试AI客户端模型节点功能
     * 该测试方法演示了如何使用装备策略工厂来处理AI客户端命令，
     * 并从应用上下文中获取OpenAiChatModel Bean实例，然后进行聊天调用。
     *
     * @throws Exception 可能抛出的异常
     */
    @Test
    public void test_aiClientModelNode() throws Exception {
        // 获取装备策略处理器
        StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> armoryStrategyHandler =
                defaultArmoryStrategyFactory.armoryStrategyHandler();

        // 构建装备命令实体并应用策略处理器
        String apply = armoryStrategyHandler.apply(
                ArmoryCommandEntity.builder()
                        .commandType(AiAgentEnumVO.AI_CLIENT.getCode()) // 设置命令类型为AI客户端
                        .commandIdList(Arrays.asList("3001")) // 设置命令ID列表
                        .build(),
                new DefaultArmoryStrategyFactory.DynamicContext()); // 创建动态上下文

        // 从应用上下文中获取OpenAiChatModel Bean实例
        OpenAiChatModel openAiChatModel = (OpenAiChatModel) applicationContext.getBean(AiAgentEnumVO.AI_CLIENT_MODEL.getBeanName("2001"));

        // 记录模型构建日志
        log.info("模型构建:{}", openAiChatModel);

        // 构建用户消息提示
        Prompt prompt = Prompt.builder()
                .messages(new UserMessage(
                        """
                                有哪些工具可以使用
                                """)) // 设置用户消息内容
                .build();

        // 调用模型获取聊天响应
        ChatResponse chatResponse = openAiChatModel.call(prompt);

        // 记录测试结果日志
        log.info("测试结果(call):{}", JSON.toJSONString(chatResponse));
    }

    /**
     * 测试AI客户端功能
     * 该测试方法演示了如何使用装备策略工厂来处理AI客户端命令，
     * 并从应用上下文中获取ChatClient Bean实例，然后进行聊天调用。
     *
     * @throws Exception 可能抛出的异常
     */
    @Test
    public void test_aiClient() throws Exception {
        // 获取装备策略处理器
        StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> armoryStrategyHandler =
                defaultArmoryStrategyFactory.armoryStrategyHandler();

        // 构建装备命令实体并应用策略处理器
        String apply = armoryStrategyHandler.apply(
                ArmoryCommandEntity.builder()
                        .commandType(AiAgentEnumVO.AI_CLIENT.getCode()) // 设置命令类型为AI客户端
                        .commandIdList(Arrays.asList("3001")) // 设置命令ID列表
                        .build(),
                new DefaultArmoryStrategyFactory.DynamicContext()); // 创建动态上下文

        // 从应用上下文中获取ChatClient Bean实例
        ChatClient chatClient = (ChatClient) applicationContext.getBean(AiAgentEnumVO.AI_CLIENT.getBeanName("3001"));
        
        // 记录客户端构建日志
        log.info("客户端构建:{}", chatClient);

        // 使用ChatClient进行聊天调用并获取响应内容
        String content = chatClient.prompt(Prompt.builder()
                .messages(new UserMessage(
                        """
                                有哪些工具可以使用
                                """)) // 设置用户消息内容
                .build()).call().content();

        // 记录测试结果日志
        log.info("测试结果(call):{}", content);
    }

}
