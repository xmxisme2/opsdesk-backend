package com.opsdesk.system.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.config.EmailNotificationProperties;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.system.dto.EmailNotificationSettingsUpdateRequest;
import com.opsdesk.system.entity.SystemConfig;
import com.opsdesk.system.mapper.SystemConfigMapper;
import com.opsdesk.system.service.EmailNotificationSettingsService;
import com.opsdesk.system.vo.EmailNotificationSettingsVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

/** 邮件通知配置实现；缺少旧配置时保持关闭，避免升级期间误发送邮件。 */
@Service
public class EmailNotificationSettingsServiceImpl implements EmailNotificationSettingsService {
    /** 配置组只允许服务端使用，不允许外部请求指定。 */
    public static final String CONFIG_GROUP = "NOTIFICATION";
    /** 邮件发送总开关，值只允许 true 或 false。 */
    public static final String KEY_ENABLED = "notification.email.enabled";
    /** 开关开启后的固定默认收件邮箱。 */
    public static final String KEY_DEFAULT_RECIPIENT = "notification.email.default_recipient";
    public static final String DEFAULT_RECIPIENT = "sean.siu@astralotus.com";

    private final SystemConfigMapper mapper;
    private final AuditLogService auditLogService;
    private final EmailNotificationProperties emailNotificationProperties;

    public EmailNotificationSettingsServiceImpl(SystemConfigMapper mapper, AuditLogService auditLogService,
                                                EmailNotificationProperties emailNotificationProperties) {
        this.mapper = mapper;
        this.auditLogService = auditLogService;
        this.emailNotificationProperties = emailNotificationProperties;
    }

    @Override
    public EmailNotificationSettingsVO detail() {
        boolean enabled = emailNotificationProperties.isEnabled();
        String recipient = DEFAULT_RECIPIENT;
        for (SystemConfig config : mapper.findByGroup(CONFIG_GROUP)) {
            if (KEY_ENABLED.equals(config.getConfigKey())) enabled = Boolean.parseBoolean(config.getConfigValue());
            if (KEY_DEFAULT_RECIPIENT.equals(config.getConfigKey()) && StringUtils.hasText(config.getConfigValue())) {
                recipient = config.getConfigValue().trim();
            }
        }
        return new EmailNotificationSettingsVO(enabled, recipient);
    }

    @Override
    @Transactional
    public EmailNotificationSettingsVO update(EmailNotificationSettingsUpdateRequest request, Long operatorId,
                                              String requestIp, String userAgent) {
        String recipient = request.defaultRecipient().trim().toLowerCase(Locale.ROOT);
        if (mapper.updateValue(KEY_DEFAULT_RECIPIENT, recipient, operatorId) != 1) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "邮件通知配置缺失，请先执行数据库迁移脚本");
        }
        auditLogService.record(operatorId, "UPDATE", "SYSTEM_CONFIG", null,
                "更新邮件通知默认收件邮箱", requestIp, userAgent);
        return new EmailNotificationSettingsVO(emailNotificationProperties.isEnabled(), recipient);
    }
}
