package com.opsdesk.comment.mapper;

import com.opsdesk.comment.entity.TicketComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单评论数据访问 Mapper。
 *
 * <p>只负责 ticket_comment 表读写和分页查询；资源范围、内部备注可见性和审计由 Service 处理。</p>
 */
@Mapper
public interface TicketCommentMapper {

    int insert(TicketComment comment);

    TicketComment findById(@Param("id") Long id);

    List<TicketComment> searchByTicketId(@Param("ticketId") Long ticketId,
                                         @Param("includeInternal") boolean includeInternal);

    int logicalDelete(@Param("id") Long id,
                      @Param("operatorId") Long operatorId);
}
