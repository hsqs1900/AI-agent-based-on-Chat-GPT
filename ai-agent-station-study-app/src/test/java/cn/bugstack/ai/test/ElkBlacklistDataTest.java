package cn.bugstack.ai.test;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 模拟向Elasticsearch写入拼团项目黑名单限流数据的测试类
 * 基于 elk-blacklist-data.sh 脚本的Java实现
 * 
 * @author jinjie
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ElkBlacklistDataTest {

    // Elasticsearch配置 - 定义Elasticsearch服务地址和索引名称前缀
    private static final String ES_HOST = "localhost:9200";
    private static final String INDEX_NAME_PREFIX = "group-buy-market-log-";
    
    // 模拟数据 - 定义用于生成模拟数据的常量数组
    private static final String[] USERS = {"user001", "user002", "user003", "user004", "user005", 
                                          "user006", "user007", "user008", "user009", "user010"};
    
    private static final String[] IPS = {"117.72.214.115", "117.72.214.115", "192.168.1.102",
                                        "10.0.0.50", "10.0.0.51", "172.16.0.100", "172.16.0.101", 
                                        "203.0.113.10", "203.0.113.11", "198.51.100.20"};
    
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)",
        "Mozilla/5.0 (Android 10; Mobile)",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"
    };
    
    private static final String[] GROUP_BUY_PRODUCTS = {"product_001", "product_002", "product_003", 
                                                       "product_004", "product_005"};
    
    private static final String[] LIMIT_REASONS = {"访问频率过高", "恶意刷单", "异常IP访问", 
                                                  "超过每日限制", "黑名单用户"};
    
    private static final String[] LIMIT_TYPES = {"rate_limit", "frequency_limit", "ip_blacklist", 
                                                "daily_limit", "user_blacklist"};
    
    private static final String[] LOG_LEVELS = {"ERROR", "WARN", "INFO"};
    
    // 创建RestTemplate实例用于HTTP请求
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 测试方法：向Elasticsearch写入黑名单限流数据
     * 主要流程：
     * 1. 构建索引名称和Elasticsearch URL
     * 2. 检查Elasticsearch连接状态
     * 3. 生成并写入50条模拟数据
     * 4. 显示写入结果和索引信息
     */
    @Test
    public void testWriteBlacklistDataToElasticsearch() {
        // 构建索引名称，格式为 group-buy-market-log-YYYY.MM.DD
        String indexName = INDEX_NAME_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        // 构建Elasticsearch基础URL
        String esUrl = "http://" + ES_HOST;
        
        // 记录测试开始日志
        log.info("开始向Elasticsearch模拟写入拼团项目黑名单限流数据...");
        log.info("目标索引: {}", indexName);
        log.info("ES地址: {}", esUrl);
        
        // 检查Elasticsearch连接，如果连接失败则直接返回
        if (!checkElasticsearchConnection(esUrl)) {
            log.error("无法连接到Elasticsearch ({})", esUrl);
            log.error("请确保Elasticsearch服务正在运行");
            return;
        }
        log.info("Elasticsearch连接正常");
        
        // 批量写入数据
        log.info("开始生成并写入模拟数据...");
        int successCount = 0; // 成功写入的数据条数计数器
        int totalCount = 50;  // 总共需要写入的数据条数
        
        // 循环生成并写入数据
        for (int i = 1; i <= totalCount; i++) {
            log.info("写入第 {} 条数据...", i);
            
            // 生成一条模拟的日志数据
            Map<String, Object> logData = generateLogData();
            
            // 将数据写入到Elasticsearch
            boolean success = writeToElasticsearch(esUrl, indexName, logData);
            
            // 根据写入结果更新计数器并记录日志
            if (success) {
                successCount++;
                log.info("第 {} 条数据写入成功", i);
            } else {
                log.error("第 {} 条数据写入失败", i);
            }
            
            // 添加随机延迟，模拟真实场景下的写入间隔
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(100, 500));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // 记录数据写入完成的总结信息
        log.info("数据写入完成！成功: {}/{}", successCount, totalCount);
        
        // 获取并显示索引中的文档数量
        getIndexCount(esUrl, indexName);
        
        // 提供查看数据的命令提示
        log.info("可以使用以下命令查看写入的数据:");
        log.info("curl -X GET \"http://{}/{}/_search?pretty&size=5\"", ES_HOST, indexName);
        log.info("或者在Kibana中查看索引: {}", indexName);
    }
    
    /**
     * 检查Elasticsearch连接
     * 通过访问Elasticsearch的集群健康状态API来检查连接是否正常
     * @param esUrl Elasticsearch的基础URL
     * @return 连接成功返回true，否则返回false
     */
    private boolean checkElasticsearchConnection(String esUrl) {
        try {
            // 发送GET请求到Elasticsearch的_cluster/health端点
            ResponseEntity<String> response = restTemplate.getForEntity(esUrl + "/_cluster/health", String.class);
            // 如果HTTP状态码为200，则表示连接成功
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            // 捕获异常并记录错误日志
            log.error("检查Elasticsearch连接失败", e);
            return false;
        }
    }
    
    /**
     * 生成模拟限流日志数据
     * 随机选择各种数据字段的值，构建一个完整的日志数据对象
     * @return 包含所有日志字段的Map对象
     */
    private Map<String, Object> generateLogData() {
        // 获取线程安全的随机数生成器
        Random random = ThreadLocalRandom.current();
        
        // 随机选择用户ID、IP地址、用户代理等基础信息
        String userId = USERS[random.nextInt(USERS.length)];
        String ip = IPS[random.nextInt(IPS.length)];
        String userAgent = USER_AGENTS[random.nextInt(USER_AGENTS.length)];
        String product = GROUP_BUY_PRODUCTS[random.nextInt(GROUP_BUY_PRODUCTS.length)];
        String limitReason = LIMIT_REASONS[random.nextInt(LIMIT_REASONS.length)];
        String limitType = LIMIT_TYPES[random.nextInt(LIMIT_TYPES.length)];
        String logLevel = LOG_LEVELS[random.nextInt(LOG_LEVELS.length)];
        
        // 生成随机时间戳（最近24小时内）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime randomTime = now.minusSeconds(random.nextInt(86400)); // 24小时内随机
        String timestamp = randomTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
        
        // 生成随机的请求次数、限制阈值等数值数据
        int requestCount = random.nextInt(100) + 50; // 50-149次请求
        int limitThreshold = random.nextInt(50) + 20; // 20-69的限制阈值
        int threadNum = random.nextInt(10) + 1;       // 线程号1-10
        int responseTime = random.nextInt(100) + 10;  // 响应时间10-109ms
        
        // 构建日志消息内容
        String message = String.format("用户访问拼团项目被限流 - 用户ID: %s, 产品: %s, 原因: %s, IP: %s, 请求次数: %d, 限制阈值: %d",
                userId, product, limitReason, ip, requestCount, limitThreshold);
        
        // 创建日志数据Map并填充所有字段
        Map<String, Object> logData = new HashMap<>();
        logData.put("@timestamp", timestamp);              // 时间戳
        logData.put("level", logLevel);                    // 日志级别
        logData.put("logger", "com.fuzhengwei.security.RateLimitFilter"); // 日志记录器
        logData.put("thread", "http-nio-8080-exec-" + threadNum);        // 线程名
        logData.put("message", message);                   // 日志消息
        logData.put("application", "group-buy-market");    // 应用名称
        logData.put("environment", "production");          // 环境
        logData.put("service", "group-buy-service");       // 服务名
        logData.put("user_id", userId);                    // 用户ID
        logData.put("ip_address", ip);                     // IP地址
        logData.put("user_agent", userAgent);              // 用户代理
        logData.put("product_id", product);                // 产品ID
        logData.put("limit_type", limitType);              // 限制类型
        logData.put("limit_reason", limitReason);          // 限制原因
        logData.put("request_count", requestCount);        // 请求次数
        logData.put("limit_threshold", limitThreshold);    // 限制阈值
        logData.put("action", "blocked");                  // 操作类型
        logData.put("endpoint", "/api/group-buy/join");    // API端点
        logData.put("method", "POST");                     // HTTP方法
        logData.put("status_code", 429);                   // 状态码
        logData.put("response_time", responseTime);        // 响应时间
        logData.put("session_id", "session_" + System.currentTimeMillis() + "_" + random.nextInt(10000)); // 会话ID
        logData.put("trace_id", "trace_" + System.currentTimeMillis() + "_" + random.nextInt(10000));     // 跟踪ID
        logData.put("tags", Arrays.asList("限流", "黑名单", "拼团", "安全")); // 标签列表
        
        return logData;
    }
    
    /**
     * 写入数据到Elasticsearch
     * 使用RestTemplate向Elasticsearch发送POST请求，将数据写入指定索引
     * @param esUrl Elasticsearch的基础URL
     * @param indexName 索引名称
     * @param logData 要写入的日志数据
     * @return 写入成功返回true，否则返回false
     */
    private boolean writeToElasticsearch(String esUrl, String indexName, Map<String, Object> logData) {
        try {
            // 构建写入文档的URL
            String url = esUrl + "/" + indexName + "/_doc";
            
            // 设置请求头，指定内容类型为JSON
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 将日志数据转换为JSON字符串
            String jsonData = JSON.toJSONString(logData);
            HttpEntity<String> request = new HttpEntity<>(jsonData, headers);
            
            // 发送POST请求写入数据
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            // 检查响应状态码和结果
            if (response.getStatusCode() == HttpStatus.CREATED) {
                String responseBody = response.getBody();
                // 验证响应中是否包含创建成功的标识
                return responseBody != null && responseBody.contains("\"result\":\"created\"");
            }
            
            return false;
        } catch (Exception e) {
            // 捕获异常并记录错误日志
            log.error("写入Elasticsearch失败", e);
            return false;
        }
    }
    
    /**
     * 获取索引文档数量
     * 通过访问Elasticsearch的_count端点获取指定索引的文档数量
     * @param esUrl Elasticsearch的基础URL
     * @param indexName 索引名称
     */
    private void getIndexCount(String esUrl, String indexName) {
        try {
            // 构建获取文档数量的URL
            String url = esUrl + "/" + indexName + "/_count";
            // 发送GET请求获取索引信息
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            // 如果响应成功，则记录索引信息
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("索引信息: {}", response.getBody());
            }
        } catch (Exception e) {
            // 捕获异常并记录警告日志
            log.warn("获取索引信息失败", e);
        }
    }
}