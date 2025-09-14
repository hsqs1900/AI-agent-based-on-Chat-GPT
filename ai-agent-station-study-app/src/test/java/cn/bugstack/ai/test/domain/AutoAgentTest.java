package cn.bugstack.ai.test.domain;

import cn.bugstack.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.bugstack.ai.domain.agent.model.entity.ExecuteCommandEntity;
import cn.bugstack.ai.domain.agent.model.valobj.enums.AiAgentEnumVO;
import cn.bugstack.ai.domain.agent.service.armory.factory.DefaultArmoryStrategyFactory;
import cn.bugstack.ai.domain.agent.service.execute.auto.step.factory.DefaultAutoAgentExecuteStrategyFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

/**
 * 自动代理测试类
 *
 * @author jinjie
 * 2025/7/27 17:52
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AutoAgentTest {

    /**
     * 注入默认的装配策略工厂，用于创建和管理AI客户端等资源
     */
    @Resource
    private DefaultArmoryStrategyFactory defaultArmoryStrategyFactory;

    /**
     * 注入默认的自动执行策略工厂，用于处理AI代理的自动执行逻辑
     */
    @Resource
    private DefaultAutoAgentExecuteStrategyFactory defaultAutoAgentExecuteStrategyFactory;

    /**
     * 注入Spring的应用上下文，用于获取Bean实例
     */
    @Resource
    private ApplicationContext applicationContext;

    /**
     * 在每个测试方法执行前运行，用于初始化测试环境
     * 主要功能是装配AI客户端资源
     *
     * @throws Exception 如果初始化过程中发生异常
     */
    @Before
    public void init() throws Exception {
        // 获取装配策略处理器
        StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> armoryStrategyHandler =
                defaultArmoryStrategyFactory.armoryStrategyHandler();

        // 应用装配命令，创建AI客户端实例
        String apply = armoryStrategyHandler.apply(
                ArmoryCommandEntity.builder()
                        // 设置命令类型为AI客户端
                        .commandType(AiAgentEnumVO.AI_CLIENT.getCode())
                        // 设置要创建的客户端ID列表
                        .commandIdList(Arrays.asList("3101", "3102", "3103"))
                        .build(),
                // 创建新的动态上下文
                new DefaultArmoryStrategyFactory.DynamicContext());

        // 从应用上下文中获取指定ID的ChatClient实例
        ChatClient chatClient = (ChatClient) applicationContext.getBean(AiAgentEnumVO.AI_CLIENT.getBeanName("3101"));
        log.info("客户端构建:{}", chatClient);
    }

    /**
     * 自动代理测试方法
     * 测试AI代理的自动执行功能
     *
     * @throws Exception 如果测试过程中发生异常
     */
    @Test
    public void autoAgent() throws Exception {
        // 获取执行策略处理器
        StrategyHandler<ExecuteCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext, String> executeHandler
                = defaultAutoAgentExecuteStrategyFactory.armoryStrategyHandler();

        // 创建执行命令实体
        ExecuteCommandEntity executeCommandEntity = new ExecuteCommandEntity();
        // 设置AI代理ID
        executeCommandEntity.setAiAgentId("3");
        // 设置执行消息/任务
        executeCommandEntity.setMessage("搜索小傅哥，技术项目列表。编写成一份文档，说明不同项目的学习目标，以及不同阶段的伙伴应该学习哪个项目。");
        // 设置会话ID，使用时间戳确保唯一性
        executeCommandEntity.setSessionId("session-id-" + System.currentTimeMillis());
        // 设置最大执行步骤数
        executeCommandEntity.setMaxStep(3);

        // 执行AI代理任务并获取结果
        String apply = executeHandler.apply(executeCommandEntity, new DefaultAutoAgentExecuteStrategyFactory.DynamicContext());
        log.info("测试结果:{}", apply);
    }

}
