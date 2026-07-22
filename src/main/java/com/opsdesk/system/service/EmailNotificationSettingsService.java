package com.opsdesk.system.service;

import com.opsdesk.system.dto.EmailNotificationSettingsUpdateRequest;
import com.opsdesk.system.vo.EmailNotificationSettingsVO;

/** 邮件通知系统配置服务，集中处理配置读取、校验和审计。 */
public interface EmailNotificationSettingsService {
    EmailNotificationSettingsVO detail();

    EmailNotificationSettingsVO update(EmailNotificationSettingsUpdateRequest request, Long operatorId,
                                       String requestIp, String userAgent);
}
