package com.opsdesk.notification.service.impl;

import com.opsdesk.notification.entity.Notification;
import com.opsdesk.notification.service.EmailNotificationSender;
import com.opsdesk.system.service.EmailNotificationSettingsService;
import com.opsdesk.system.vo.EmailNotificationSettingsVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 默认邮件通知发送器。
 *
 * <p>配置开关关闭、SMTP 未配置或投递失败时只记录日志，站内通知已经写入后不受影响。</p>
 */
@Service
public class EmailNotificationSenderImpl implements EmailNotificationSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationSenderImpl.class);
    private static final String SUBJECT_PREFIX = "[OpsDesk] ";

    private final EmailNotificationSettingsService settingsService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public EmailNotificationSenderImpl(EmailNotificationSettingsService settingsService,
                                       ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.settingsService = settingsService;
        this.mailSenderProvider = mailSenderProvider;
    }

    @Override
    public void send(Notification notification) {
        try {
            EmailNotificationSettingsVO settings = settingsService.detail();
            if (!settings.enabled()) return;
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                LOGGER.warn("邮件通知已开启但 SMTP 未配置，已跳过投递：notificationId={}", notification.getId());
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(settings.defaultRecipient());
            message.setSubject(SUBJECT_PREFIX + notification.getTitle());
            message.setText(notification.getContent());
            mailSender.send(message);
        } catch (Exception exception) {
            LOGGER.warn("邮件通知投递失败，站内通知不受影响：notificationId={}", notification.getId(), exception);
        }
    }
}
