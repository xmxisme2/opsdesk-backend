package com.opsdesk.ticket.mapper;

import com.opsdesk.ticket.entity.TicketWatch;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 工单关注关系数据访问 Mapper。
 *
 * <p>用于详情页关注按钮和“我关注”范围查询，保持逻辑删除以保留用户操作痕迹。</p>
 */
@Mapper
public interface TicketWatchMapper {

    @Select("""
            SELECT COUNT(1)
            FROM ticket_watch
            WHERE ticket_id = #{ticketId}
              AND user_id = #{userId}
              AND deleted = 0
            """)
    int countActive(@Param("ticketId") Long ticketId,
                    @Param("userId") Long userId);

    @Insert("""
            INSERT INTO ticket_watch (
              id, ticket_id, user_id, create_by, update_by, deleted
            )
            VALUES (
              #{id}, #{ticketId}, #{userId}, #{createBy}, #{updateBy}, 0
            )
            """)
    int insert(TicketWatch watch);

    @Update("""
            UPDATE ticket_watch
            SET deleted = 0,
                update_by = #{updateBy},
                update_time = CURRENT_TIMESTAMP
            WHERE ticket_id = #{ticketId}
              AND user_id = #{userId}
              AND deleted = 1
            """)
    int restoreDeleted(TicketWatch watch);

    @Update("""
            UPDATE ticket_watch
            SET deleted = 1,
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE ticket_id = #{ticketId}
              AND user_id = #{userId}
              AND deleted = 0
            """)
    int logicalDelete(@Param("ticketId") Long ticketId,
                      @Param("userId") Long userId,
                      @Param("operatorId") Long operatorId);
}
