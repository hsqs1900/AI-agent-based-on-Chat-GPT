package cn.bugstack.ai.domain.agent.service.execute;

import cn.bugstack.ai.domain.agent.model.entity.ExecuteCommandEntity;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

/**
 * 执行策略接口
 *
 * @author jinjie
 * 2025/8/5 09:48
 */
public interface IExecuteStrategy {

    void execute(ExecuteCommandEntity requestParameter, ResponseBodyEmitter emitter) throws Exception;

}
