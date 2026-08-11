package com.opsdesk.ticket.mapper;

import com.opsdesk.ticket.entity.TicketOperationLog;
import com.opsdesk.ticket.vo.TicketTimelineItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单操作日志数据访问 Mapper。
 *
 * <p>只写入和读取 ticket_operation_log，审计日志与跨模块事件由后续 notification/audit 模块扩展。</p>
 */
@Mapper
public interface TicketOperationLogMapper {

    int insert(TicketOperationLog log);

    List<TicketOperationLog> searchByTicketId(@Param("ticketId") Long ticketId);

    /** 聚合工单操作、评论和附件事件；普通提交人不可读取内部备注。 */
    List<TicketTimelineItemVO> searchTimeline(@Param("ticketId") Long ticketId,
                                              @Param("includeInternal") boolean includeInternal);
}
