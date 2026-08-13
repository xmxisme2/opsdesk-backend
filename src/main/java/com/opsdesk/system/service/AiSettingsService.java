package com.opsdesk.system.service;

import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.system.dto.AiSettingsUpdateRequest;
import com.opsdesk.system.vo.AiSettingsVO;

/** AI 公开运行设置查询服务。 */
public interface AiSettingsService {
    AiSettingsVO detail(CurrentUser currentUser);

    /** 更新运行时开关并记录系统配置审计日志。 */
    AiSettingsVO update(AiSettingsUpdateRequest request, CurrentUser currentUser,
                        String requestIp, String userAgent);
}
