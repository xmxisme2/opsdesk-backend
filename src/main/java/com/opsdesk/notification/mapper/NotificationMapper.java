package com.opsdesk.notification.mapper;

import com.opsdesk.notification.entity.Notification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    @Select("""
            SELECT *
            FROM notification
            WHERE id = #{id}
              AND deleted = 0
            LIMIT 1
            """)
    Notification findById(@Param("id") Long id);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM notification
            WHERE receiver_id = #{receiverId}
              AND deleted = 0
            <if test="read != null">
              AND read_status = CASE WHEN #{read} = TRUE THEN 1 ELSE 0 END
            </if>
            <if test="type != null and type != ''">
              AND type = #{type}
            </if>
            <if test="createdFrom != null">
              AND create_time &gt;= #{createdFrom}
            </if>
            <if test="createdTo != null">
              AND create_time &lt;= #{createdTo}
            </if>
            </script>
            """)
    long countSearch(@Param("receiverId") Long receiverId,
                     @Param("read") Boolean read,
                     @Param("type") String type,
                     @Param("createdFrom") LocalDateTime createdFrom,
                     @Param("createdTo") LocalDateTime createdTo);

    @Select("""
            <script>
            SELECT *
            FROM notification
            WHERE receiver_id = #{receiverId}
              AND deleted = 0
            <if test="read != null">
              AND read_status = CASE WHEN #{read} = TRUE THEN 1 ELSE 0 END
            </if>
            <if test="type != null and type != ''">
              AND type = #{type}
            </if>
            <if test="createdFrom != null">
              AND create_time &gt;= #{createdFrom}
            </if>
            <if test="createdTo != null">
              AND create_time &lt;= #{createdTo}
            </if>
            ORDER BY create_time DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<Notification> search(@Param("receiverId") Long receiverId,
                              @Param("read") Boolean read,
                              @Param("type") String type,
                              @Param("createdFrom") LocalDateTime createdFrom,
                              @Param("createdTo") LocalDateTime createdTo,
                              @Param("offset") long offset,
                              @Param("size") long size);

    @Select("""
            SELECT COUNT(1)
            FROM notification
            WHERE receiver_id = #{receiverId}
              AND read_status = 0
              AND deleted = 0
            """)
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
