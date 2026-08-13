package com.opsdesk.ai.service;

import com.opsdesk.ai.vo.AiRuntimeConfigVO;
import com.opsdesk.common.security.CurrentUser;

/** 主应用代理 AI 独立服务运行配置，禁止透传任何密钥类字段。 */
public interface AiRuntimeConfigProxyService {
    AiRuntimeConfigVO detail(CurrentUser currentUser);

    AiRuntimeConfigVO update(boolean enabled, boolean ragEnabled, CurrentUser currentUser);
}
