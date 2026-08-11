package com.opsdesk.system.service;

import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.system.vo.AiSettingsVO;

/** AI 公开运行设置查询服务。 */
public interface AiSettingsService {
    AiSettingsVO detail(CurrentUser currentUser);
}
