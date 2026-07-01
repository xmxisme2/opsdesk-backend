package com.opsdesk.comment.mapper;

import com.opsdesk.comment.entity.TicketComment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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

    TicketComment findById(@Param("id") Long id);

    List<TicketComment> searchByTicketId(@Param("ticketId") Long ticketId,
                                         @Param("includeInternal") boolean includeInternal);

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
