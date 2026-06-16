package com.opsdesk.ticket.mapper;

import com.opsdesk.ticket.entity.TicketOperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工单操作日志数据访问 Mapper。
 *
 * <p>只写入和读取 ticket_operation_log，审计日志与跨模块事件由后续 notification/audit 模块扩展。</p>
 */
@Mapper
public interface TicketOperationLogMapper {

    @Insert("""
            INSERT INTO ticket_operation_log (
              id, ticket_id, operation_type, from_status, to_status, operator_id,
              content, request_ip, user_agent, create_by, update_by, deleted
            )
            VALUES (
              #{id}, #{ticketId}, #{operationType}, #{fromStatus}, #{toStatus}, #{operatorId},
              #{content}, #{requestIp}, #{userAgent}, #{createBy}, #{updateBy}, 0
            )
            """)
    int insert(TicketOperationLog log);

    @Select("""
            SELECT COUNT(1)
            FROM ticket_operation_log
            WHERE ticket_id = #{ticketId}
              AND deleted = 0
            """)
    long countByTicketId(@Param("ticketId") Long ticketId);

    @Select("""
            SELECT *
            FROM ticket_operation_log
            WHERE ticket_id = #{ticketId}
              AND deleted = 0
            ORDER BY create_time DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<TicketOperationLog> searchByTicketId(@Param("ticketId") Long ticketId,
                                              @Param("offset") long offset,
                                              @Param("size") long size);
}
