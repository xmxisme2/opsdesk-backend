package com.opsdesk.comment.mapper;

import com.opsdesk.comment.entity.TicketComment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 工单评论数据访问 Mapper。
 *
 * <p>只负责 ticket_comment 表读写和分页查询；资源范围、内部备注可见性和审计由 Service 处理。</p>
 */
@Mapper
public interface TicketCommentMapper {

    @Insert("""
            INSERT INTO ticket_comment (
              id, ticket_id, content, comment_type, author_id, create_by, update_by, deleted
            )
            VALUES (
              #{id}, #{ticketId}, #{content}, #{commentType}, #{authorId}, #{createBy}, #{updateBy}, 0
            )
            """)
    int insert(TicketComment comment);

    @Select("""
            SELECT c.*,
                   COALESCE(NULLIF(u.nickname, ''), NULLIF(u.username, ''), u.phone) AS author_name
            FROM ticket_comment c
            LEFT JOIN sys_user u ON u.id = c.author_id AND u.deleted = 0
            WHERE c.id = #{id}
              AND c.deleted = 0
            LIMIT 1
            """)
    TicketComment findById(@Param("id") Long id);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM ticket_comment
            WHERE ticket_id = #{ticketId}
              AND deleted = 0
            <if test="includeInternal == false">
              AND comment_type != 'INTERNAL'
            </if>
            </script>
            """)
    long countByTicketId(@Param("ticketId") Long ticketId,
                         @Param("includeInternal") boolean includeInternal);

    @Select("""
            <script>
            SELECT c.*,
                   COALESCE(NULLIF(u.nickname, ''), NULLIF(u.username, ''), u.phone) AS author_name
            FROM ticket_comment c
            LEFT JOIN sys_user u ON u.id = c.author_id AND u.deleted = 0
            WHERE c.ticket_id = #{ticketId}
              AND c.deleted = 0
            <if test="includeInternal == false">
              AND c.comment_type != 'INTERNAL'
            </if>
            ORDER BY c.create_time ASC, c.id ASC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<TicketComment> searchByTicketId(@Param("ticketId") Long ticketId,
                                         @Param("includeInternal") boolean includeInternal,
                                         @Param("offset") long offset,
                                         @Param("size") long size);

    @Update("""
            UPDATE ticket_comment
            SET deleted = 1,
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND deleted = 0
            """)
    int logicalDelete(@Param("id") Long id,
                      @Param("operatorId") Long operatorId);
}
