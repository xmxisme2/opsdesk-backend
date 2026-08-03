package com.opsdesk.ai.service;

import com.opsdesk.ai.dto.AiConnectionTestRequest;
import com.opsdesk.ai.vo.AiConnectionTestVO;
import com.opsdesk.ai.vo.AiServiceHealthVO;
import com.opsdesk.common.security.CurrentUser;

/**
 * 主应用 AI 健康代理服务。
 */
public interface AiHealthProxyService {

    AiServiceHealthVO check(CurrentUser currentUser);

    /** 使用服务身份代理模型或基础连接真实测试。 */
    AiConnectionTestVO testConnection(AiConnectionTestRequest request, CurrentUser currentUser);
}
