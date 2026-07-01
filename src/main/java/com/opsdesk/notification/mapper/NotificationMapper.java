package com.opsdesk.notification.mapper;

import com.opsdesk.notification.entity.Notification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内通知数据访问 Mapper。
 *
 * <p>只负责 notification 表读写；接收人范围和缓存刷新由 Service 处理。</p>
 */
@Mapper
public interface NotificationMapper {

    @Insert("""
            INSERT INTO notification (
              id, receiver_id, type, title, content, biz_type, biz_id, read_status,
              read_time, create_by, update_by, deleted
            )
            VALUES (
              #{id}, #{receiverId}, #{type}, #{title}, #{content}, #{bizType}, #{bizId}, #{readStatus},
              #{readTime}, #{createBy}, #{updateBy}, 0
            )
            """)
    int insert(Notification notification);

    Notification findById(@Param("id") Long id);

    List<Notification> search(@Param("receiverId") Long receiverId,
                              @Param("read") Boolean read,
                              @Param("type") String type,
                              @Param("createdFrom") LocalDateTime createdFrom,
                              @Param("createdTo") LocalDateTime createdTo);

    long countUnread(@Param("receiverId") Long receiverId);

    @Update("""
            UPDATE notification
            SET read_status = 1,
                read_time = CURRENT_TIMESTAMP,
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND receiver_id = #{receiverId}
              AND read_status = 0
              AND deleted = 0
            """)
    int markRead(@Param("id") Long id,
                 @Param("receiverId") Long receiverId,
                 @Param("operatorId") Long operatorId);

    @Update("""
            <script>
            UPDATE notification
            SET read_status = 1,
                read_time = CURRENT_TIMESTAMP,
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE receiver_id = #{receiverId}
              AND read_status = 0
              AND deleted = 0
            <if test="type != null and type != ''">
              AND type = #{type}
            </if>
            </script>
            """)
    int markAllRead(@Param("receiverId") Long receiverId,
                    @Param("type") String type,
                    @Param("operatorId") Long operatorId);
}
