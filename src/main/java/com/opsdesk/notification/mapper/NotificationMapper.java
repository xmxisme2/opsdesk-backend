package com.opsdesk.notification.mapper;

import com.opsdesk.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内通知数据访问 Mapper。
 *
 * <p>只负责 notification 表读写；接收人范围和缓存刷新由 Service 处理。</p>
 */
@Mapper
public interface NotificationMapper {

    int insert(Notification notification);

    Notification findById(@Param("id") Long id);

    List<Notification> search(@Param("receiverId") Long receiverId,
                              @Param("read") Boolean read,
                              @Param("type") String type,
                              @Param("createdFrom") LocalDateTime createdFrom,
                              @Param("createdTo") LocalDateTime createdTo);

    long countUnread(@Param("receiverId") Long receiverId);

    List<Notification> findLatestByReceiver(@Param("receiverId") Long receiverId,
                                            @Param("limit") int limit);

    int markRead(@Param("id") Long id,
                 @Param("receiverId") Long receiverId,
                 @Param("operatorId") Long operatorId);

    int markAllRead(@Param("receiverId") Long receiverId,
                    @Param("type") String type,
                    @Param("operatorId") Long operatorId);
}
