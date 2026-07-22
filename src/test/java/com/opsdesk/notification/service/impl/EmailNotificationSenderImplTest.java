package com.opsdesk.notification.service.impl;

import com.opsdesk.notification.entity.Notification;
import com.opsdesk.system.service.EmailNotificationSettingsService;
import com.opsdesk.system.vo.EmailNotificationSettingsVO;
import com.opsdesk.config.EmailNotificationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 邮件通知发送器测试，确保关闭开关或缺失 SMTP 时不会破坏通知主流程。 */
class EmailNotificationSenderImplTest {

    @Test
    void shouldSendToConfiguredDefaultRecipientWhenEnabled() {
        EmailNotificationSettingsService settingsService = mock(EmailNotificationSettingsService.class);
        JavaMailSender mailSender = mock(JavaMailSender.class);
        @SuppressWarnings("unchecked") ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        EmailNotificationProperties properties = enabledProperties();
        when(settingsService.detail()).thenReturn(new EmailNotificationSettingsVO(true, "sean.siu@astralotus.com"));
        when(provider.getIfAvailable()).thenReturn(mailSender);

        new EmailNotificationSenderImpl(settingsService, properties, provider).send(notification());

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldSkipWhenDisabled() {
        EmailNotificationSettingsService settingsService = mock(EmailNotificationSettingsService.class);
        @SuppressWarnings("unchecked") ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        EmailNotificationProperties properties = disabledProperties();
        when(settingsService.detail()).thenReturn(new EmailNotificationSettingsVO(false, "sean.siu@astralotus.com"));

        new EmailNotificationSenderImpl(settingsService, properties, provider).send(notification());

        verify(provider, never()).getIfAvailable();
    }

    private Notification notification() {
        Notification notification = new Notification();
        notification.setId(100L);
        notification.setTitle("工单状态已变更");
        notification.setContent("工单 TK202607220001 已处理完成。");
        return notification;
    }

    /** 测试显式控制 YAML 对应的运行开关，避免依赖系统配置中的历史值。 */
    private EmailNotificationProperties enabledProperties() {
        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.setEnabled(true);
        return properties;
    }

    private EmailNotificationProperties disabledProperties() {
        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.setEnabled(false);
        return properties;
    }
}
