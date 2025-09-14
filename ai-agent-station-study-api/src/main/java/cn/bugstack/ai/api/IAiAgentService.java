package cn.bugstack.ai.api;

import cn.bugstack.ai.api.dto.AutoAgentRequestDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

/**
 * Ai Agent 服务接口
 *
 * @author jinjie
 * 2025/8/7 17:52
 */
public interface IAiAgentService {

    ResponseBodyEmitter autoAgent(AutoAgentRequestDTO request, HttpServletResponse response);

}
