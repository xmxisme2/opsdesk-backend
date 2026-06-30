package com.opsdesk.notification.converter;

import com.opsdesk.notification.entity.Notification;
import com.opsdesk.notification.vo.NotificationVO;
import org.springframework.stereotype.Component;

/**
 * 通知对象转换器。
 *
 * <p>负责 Entity 到 VO 的 ID 字符串化和 read_status 布尔值转换。</p>
 */
@Component
public class NotificationConverter {

    public NotificationVO toVO(Notification notification) {
        if (notification == null) {
            return null;
        }
        return new NotificationVO(
                String.valueOf(notification.getId()),
                String.valueOf(notification.getReceiverId()),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getBizType(),
                notification.getBizId() == null ? null : String.valueOf(notification.getBizId()),
                notification.getReadStatus() != null && notification.getReadStatus() == 1,
                notification.getReadTime(),
                notification.getCreateTime()
        );
    }
}
