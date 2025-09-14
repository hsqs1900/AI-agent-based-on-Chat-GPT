package cn.bugstack.ai.test.dao;

import cn.bugstack.ai.infrastructure.dao.IAiAgentDao;
import cn.bugstack.ai.infrastructure.dao.po.AiAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI智能体配置表 DAO 测试
 * @author bugstack虫洞栈
 * @description AI智能体配置表数据访问对象测试
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AiAgentDaoTest {

    // 注入IAiAgentDao接口，用于执行数据库操作
    @Resource
    private IAiAgentDao aiAgentDao;

    /**
     * 测试插入AI智能体配置
     * 创建一个AiAgent对象并插入到数据库中
     */
    @Test
    public void test_insert() {
        // 使用建造者模式创建AiAgent对象
        AiAgent aiAgent = AiAgent.builder()
                .agentId("test_001")              // 设置智能体ID
                .agentName("测试智能体")           // 设置智能体名称
                .description("这是一个测试智能体") // 设置描述
                .channel("agent")                 // 设置渠道类型
                .status(1)                        // 设置状态为启用
                .createTime(LocalDateTime.now())  // 设置创建时间
                .updateTime(LocalDateTime.now())  // 设置更新时间
                .build();

        // 调用DAO的insert方法插入数据
        int result = aiAgentDao.insert(aiAgent);
        // 记录插入结果和生成的主键ID
        log.info("插入结果: {}, 生成ID: {}", result, aiAgent.getId());
    }

    /**
     * 测试根据ID更新AI智能体配置
     * 根据主键ID更新对应的智能体信息
     */
    @Test
    public void test_updateById() {
        // 使用建造者模式创建AiAgent对象，指定要更新的ID
        AiAgent aiAgent = AiAgent.builder()
                .id(1L)                           // 指定要更新的记录ID
                .agentId("test_001")              // 设置智能体ID
                .agentName("更新后的测试智能体")   // 更新智能体名称
                .description("这是一个更新后的测试智能体") // 更新描述
                .channel("chat_stream")           // 更新渠道类型
                .status(1)                        // 设置状态为启用
                .updateTime(LocalDateTime.now())  // 更新时间
                .build();

        // 调用DAO的updateById方法更新数据
        int result = aiAgentDao.updateById(aiAgent);
        // 记录更新结果
        log.info("更新结果: {}", result);
    }

    /**
     * 测试根据AgentId更新AI智能体配置
     * 根据智能体ID更新对应的智能体信息
     */
    @Test
    public void test_updateByAgentId() {
        // 使用建造者模式创建AiAgent对象，通过agentId进行更新
        AiAgent aiAgent = AiAgent.builder()
                .agentId("test_001")                    // 指定要更新的智能体ID
                .agentName("通过AgentId更新的智能体")    // 更新智能体名称
                .description("通过AgentId更新的描述")    // 更新描述
                .channel("agent")                       // 更新渠道类型
                .status(0)                              // 设置状态为禁用
                .updateTime(LocalDateTime.now())        // 更新时间
                .build();

        // 调用DAO的updateByAgentId方法更新数据
        int result = aiAgentDao.updateByAgentId(aiAgent);
        // 记录更新结果
        log.info("通过AgentId更新结果: {}", result);
    }

    /**
     * 测试根据ID查询AI智能体配置
     * 根据主键ID查询对应的智能体信息
     */
    @Test
    public void test_queryById() {
        // 调用DAO的queryById方法根据ID查询数据
        AiAgent aiAgent = aiAgentDao.queryById(1L);
        // 记录查询结果
        log.info("根据ID查询结果: {}", aiAgent);
    }

    /**
     * 测试根据AgentId查询AI智能体配置
     * 根据智能体ID查询对应的智能体信息
     */
    @Test
    public void test_queryByAgentId() {
        // 调用DAO的queryByAgentId方法根据AgentId查询数据
        AiAgent aiAgent = aiAgentDao.queryByAgentId("1");
        // 记录查询结果
        log.info("根据AgentId查询结果: {}", aiAgent);
    }

    /**
     * 测试查询所有启用的AI智能体配置
     * 查询状态为启用的所有智能体列表
     */
    @Test
    public void test_queryEnabledAgents() {
        // 调用DAO的queryEnabledAgents方法查询所有启用的智能体
        List<AiAgent> aiAgents = aiAgentDao.queryEnabledAgents();
        // 记录查询结果数量
        log.info("查询启用的智能体数量: {}", aiAgents.size());
        // 遍历并记录每个启用的智能体信息
        aiAgents.forEach(agent -> log.info("启用的智能体: {}", agent));
    }

    /**
     * 测试根据渠道类型查询AI智能体配置
     * 根据指定渠道类型查询对应的智能体列表
     */
    @Test
    public void test_queryByChannel() {
        // 调用DAO的queryByChannel方法根据渠道类型查询数据
        List<AiAgent> aiAgents = aiAgentDao.queryByChannel("agent");
        // 记录查询结果数量
        log.info("根据渠道查询结果数量: {}", aiAgents.size());
        // 遍历并记录每个渠道智能体信息
        aiAgents.forEach(agent -> log.info("渠道智能体: {}", agent));
    }

    /**
     * 测试查询所有AI智能体配置
     * 查询数据库中所有的智能体配置信息
     */
    @Test
    public void test_queryAll() {
        // 调用DAO的queryAll方法查询所有智能体
        List<AiAgent> aiAgents = aiAgentDao.queryAll();
        // 记录查询结果数量
        log.info("查询所有智能体数量: {}", aiAgents.size());
        // 遍历并记录每个智能体信息
        aiAgents.forEach(agent -> log.info("智能体: {}", agent));
    }

    /**
     * 测试根据ID删除AI智能体配置
     * 根据主键ID删除对应的智能体记录
     */
    @Test
    public void test_deleteById() {
        // 调用DAO的deleteById方法根据ID删除数据
        int result = aiAgentDao.deleteById(1L);
        // 记录删除结果
        log.info("根据ID删除结果: {}", result);
    }

    /**
     * 测试根据AgentId删除AI智能体配置
     * 根据智能体ID删除对应的智能体记录
     */
    @Test
    public void test_deleteByAgentId() {
        // 调用DAO的deleteByAgentId方法根据AgentId删除数据
        int result = aiAgentDao.deleteByAgentId("test_001");
        // 记录删除结果
        log.info("根据AgentId删除结果: {}", result);
    }

}