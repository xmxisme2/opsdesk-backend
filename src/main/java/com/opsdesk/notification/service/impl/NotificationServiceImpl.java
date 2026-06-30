package com.opsdesk.notification.service.impl;

import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.common.util.IdParser;
import com.opsdesk.notification.converter.NotificationConverter;
import com.opsdesk.notification.dto.NotificationReadAllRequest;
import com.opsdesk.notification.dto.NotificationSearchRequest;
import com.opsdesk.notification.entity.Notification;
import com.opsdesk.notification.mapper.NotificationMapper;
import com.opsdesk.notification.service.NotificationService;
import com.opsdesk.notification.vo.NotificationReadAllVO;
import com.opsdesk.notification.vo.NotificationUnreadCountVO;
import com.opsdesk.notification.vo.NotificationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 通知中心服务实现。
 *
 * <p>当前实现站内通知基础能力；Redis 仅缓存未读数量，读状态变化后删除缓存并由下次读取重建。</p>
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    /** 未读数缓存 Key 前缀：按用户隔离通知红点，不允许外部传入。 */
    private static final String UNREAD_KEY_PREFIX = "notification:unread:";

    /** 未读数缓存时长：10 分钟，读状态变更后主动删除。 */
    private static final Duration UNREAD_CACHE_TTL = Duration.ofMinutes(10);

    /** 允许的通知类型：来自接口契约，外部筛选或写入必须使用这些编码。 */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "TICKET_ASSIGNED",
            "TICKET_COMMENTED",
            "TICKET_STATUS_CHANGED",
            "TICKET_OVERDUE",
            "TICKET_CLOSED"
    );

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NotificationMapper notificationMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final NotificationConverter notificationConverter;

    @Autowired
    public NotificationServiceImpl(NotificationMapper notificationMapper,
                                   StringRedisTemplate stringRedisTemplate) {
        this(notificationMapper, stringRedisTemplate, new NotificationConverter());
    }

    public NotificationServiceImpl(NotificationMapper notificationMapper,
                                   StringRedisTemplate stringRedisTemplate,
                                   NotificationConverter notificationConverter) {
        this.notificationMapper = notificationMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.notificationConverter = notificationConverter;
    }

    @Override
    public PageResult<NotificationVO> search(NotificationSearchRequest request, CurrentUser currentUser) {
        Long receiverId = requireUserId(currentUser);
        NotificationSearchRequest safeRequest = request == null ? new NotificationSearchRequest() : request;
        long page = safeRequest.normalizedPage();
        long size = safeRequest.normalizedSize();
        String type = normalizeOptionalType(safeRequest.getType());
        LocalDateTime createdFrom = parseOptionalDateTime(safeRequest.getCreatedFrom(), "创建开始时间");
        LocalDateTime createdTo = parseOptionalDateTime(safeRequest.getCreatedTo(), "创建结束时间");

        long total = notificationMapper.countSearch(receiverId, safeRequest.getRead(), type, createdFrom, createdTo);
        if (total == 0) {
            return PageResult.empty(page, size);
        }
        long offset = (page - 1) * size;
        List<NotificationVO> records = notificationMapper.search(receiverId, safeRequest.getRead(), type,
                        createdFrom, createdTo, offset, size)
                .stream()
                .map(notificationConverter::toVO)
                .toList();
        return new PageResult<>(records, page, size, total);
    }

    @Override
    public NotificationUnreadCountVO unreadCount(CurrentUser currentUser) {
        Long receiverId = requireUserId(currentUser);
        String key = unreadKey(receiverId);
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cached)) {
            try {
                return new NotificationUnreadCountVO(Long.parseLong(cached));
            } catch (NumberFormatException ignored) {
                stringRedisTemplate.delete(key);
            }
        }
        long count = notificationMapper.countUnread(receiverId);
        stringRedisTemplate.opsForValue().set(key, String.valueOf(count), UNREAD_CACHE_TTL);
        return new NotificationUnreadCountVO(count);
    }

    @Override
    public NotificationVO markRead(String id, CurrentUser currentUser) {
        Long receiverId = requireUserId(currentUser);
        Long notificationId = IdParser.parseRequired(id, "通知ID");
        Notification notification = requireNotification(notificationId);
        ensureReceiver(notification, receiverId);
        if (notification.getReadStatus() == null || notification.getReadStatus() == 0) {
            notificationMapper.markRead(notificationId, receiverId, receiverId);
            stringRedisTemplate.delete(unreadKey(receiverId));
            notification.setReadStatus(1);
            notification.setReadTime(LocalDateTime.now());
        }
        return notificationConverter.toVO(notification);
    }

    @Override
    public NotificationReadAllVO readAll(NotificationReadAllRequest request, CurrentUser currentUser) {
        Long receiverId = requireUserId(currentUser);
        String type = normalizeOptionalType(request == null ? null : request.getType());
        int updatedCount = notificationMapper.markAllRead(receiverId, type, receiverId);
        stringRedisTemplate.delete(unreadKey(receiverId));
        return new NotificationReadAllVO(updatedCount);
    }

    private Notification requireNotification(Long notificationId) {
        Notification notification = notificationMapper.findById(notificationId);
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在");
        }
        return notification;
    }

    private void ensureReceiver(Notification notification, Long receiverId) {
        if (!receiverId.equals(notification.getReceiverId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无权访问该通知");
        }
    }

    private String normalizeOptionalType(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "通知类型不正确");
        }
        return normalized;
    }

    private LocalDateTime parseOptionalDateTime(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        try {
            return text.contains("T") ? LocalDateTime.parse(text) : LocalDateTime.parse(text, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + "格式不正确");
        }
    }

    private Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }
        return currentUser.getUserId();
    }

    private String unreadKey(Long receiverId) {
        return UNREAD_KEY_PREFIX + receiverId;
    }
}
