package com.opsdesk.notification.service.impl;

import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.notification.dto.NotificationReadAllRequest;
import com.opsdesk.notification.dto.NotificationSearchRequest;
import com.opsdesk.notification.entity.Notification;
import com.opsdesk.notification.mapper.NotificationMapper;
import com.opsdesk.notification.vo.NotificationUnreadCountVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通知中心服务测试。
 *
 * <p>覆盖当前用户隔离、未读数缓存、标记已读和全部已读，确保站内通知不会越权读取。</p>
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationMapper, stringRedisTemplate);
    }

    @Test
    void searchShouldOnlyReturnCurrentUserNotifications() {
        NotificationSearchRequest request = new NotificationSearchRequest();
        request.setRead(false);
        when(notificationMapper.search(10L, false, null, null, null)).thenReturn(List.of(notification(100L, 10L)));

        PageResult<?> result = notificationService.search(request, user(10L));

        assertThat(result.total()).isEqualTo(1);
        verify(notificationMapper).search(10L, false, null, null, null);
    }

    @Test
    void unreadCountShouldUseRedisCacheWhenPresent() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("notification:unread:10")).thenReturn("7");

        NotificationUnreadCountVO result = notificationService.unreadCount(user(10L));

        assertThat(result.count()).isEqualTo(7);
    }

    @Test
    void markReadShouldRejectOtherUserNotification() {
        when(notificationMapper.findById(100L)).thenReturn(notification(100L, 20L));

        assertThatThrownBy(() -> notificationService.markRead("100", user(10L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void readAllShouldUpdateCurrentUserAndRefreshCache() {
        NotificationReadAllRequest request = new NotificationReadAllRequest();
        request.setType("TICKET_COMMENTED");
        when(notificationMapper.markAllRead(10L, "TICKET_COMMENTED", 10L)).thenReturn(3);

        int updatedCount = notificationService.readAll(request, user(10L)).updatedCount();

        assertThat(updatedCount).isEqualTo(3);
        verify(stringRedisTemplate).delete("notification:unread:10");
    }

    @Test
    void createTicketNotificationShouldInsertAndRefreshUnreadCache() {
        notificationService.createTicketNotification(10L, "TICKET_ASSIGNED", "工单已分派",
                "工单 TK202606220001 已分派给你", 200L, 1L);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getReceiverId()).isEqualTo(10L);
        assertThat(notificationCaptor.getValue().getType()).isEqualTo("TICKET_ASSIGNED");
        assertThat(notificationCaptor.getValue().getBizType()).isEqualTo("TICKET");
        assertThat(notificationCaptor.getValue().getBizId()).isEqualTo(200L);
        assertThat(notificationCaptor.getValue().getReadStatus()).isZero();
        verify(stringRedisTemplate).delete("notification:unread:10");
    }

    private Notification notification(Long id, Long receiverId) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setReceiverId(receiverId);
        notification.setType("TICKET_COMMENTED");
        notification.setTitle("工单有新评论");
        notification.setContent("你关注的工单有新评论。");
        notification.setBizType("TICKET");
        notification.setBizId(200L);
        notification.setReadStatus(0);
        notification.setCreateTime(LocalDateTime.of(2026, 6, 30, 18, 0));
        notification.setUpdateTime(notification.getCreateTime());
        return notification;
    }

    private CurrentUser user(Long userId) {
        return new CurrentUser(userId, "13800000000", "user" + userId, List.of("USER"), List.of());
    }
}
