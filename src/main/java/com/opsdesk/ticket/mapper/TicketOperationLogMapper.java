package com.opsdesk.ticket.mapper;

import com.opsdesk.ticket.entity.TicketOperationLog;
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
}
