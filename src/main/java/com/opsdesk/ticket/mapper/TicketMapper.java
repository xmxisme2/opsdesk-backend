package com.opsdesk.ticket.mapper;

import com.opsdesk.ticket.entity.Ticket;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单主表数据访问 Mapper。
 *
 * <p>只负责 ticket 表读写和列表筛选，状态流转、资源范围语义和编号生成由 Service 处理。</p>
 */
@Mapper
public interface TicketMapper {

    Ticket findById(@Param("id") Long id);

    int countByTicketNo(@Param("ticketNo") String ticketNo);

    int countByCategoryId(@Param("categoryId") Long categoryId);

    List<Ticket> search(@Param("scope") String scope,
                        @Param("ticketNo") String ticketNo,
                        @Param("keyword") String keyword,
                        @Param("status") String status,
                        @Param("priority") String priority,
                        @Param("categoryId") Long categoryId,
                        @Param("creatorId") Long creatorId,
                        @Param("assigneeId") Long assigneeId,
                        @Param("teamId") Long teamId,
                        @Param("overdue") Integer overdue,
                        @Param("createdFrom") LocalDateTime createdFrom,
                        @Param("createdTo") LocalDateTime createdTo,
                        @Param("currentUserId") Long currentUserId,
                        @Param("admin") boolean admin);

    @Insert("""
            INSERT INTO ticket (
              id, ticket_no, title, description, category_id, priority, status, creator_id,
              assignee_id, team_id, due_time, completed_time, closed_time, overdue, tags,
              create_by, update_by, deleted
            )
            VALUES (
              #{id}, #{ticketNo}, #{title}, #{description}, #{categoryId}, #{priority}, #{status}, #{creatorId},
              #{assigneeId}, #{teamId}, #{dueTime}, #{completedTime}, #{closedTime}, #{overdue}, #{tags},
              #{createBy}, #{updateBy}, 0
            )
            """)
    int insert(Ticket ticket);

    @Update("""
            UPDATE ticket
            SET ticket_no = #{ticketNo},
                title = #{title},
                description = #{description},
                category_id = #{categoryId},
                priority = #{priority},
                status = #{status},
                assignee_id = #{assigneeId},
                team_id = #{teamId},
                due_time = #{dueTime},
                completed_time = #{completedTime},
                closed_time = #{closedTime},
                overdue = #{overdue},
                tags = #{tags},
                update_by = #{updateBy},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND deleted = 0
            """)
    int update(Ticket ticket);
}
