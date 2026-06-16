package com.opsdesk.ticket.mapper;

import com.opsdesk.ticket.entity.Ticket;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    @Select("""
            SELECT *
            FROM ticket
            WHERE id = #{id}
              AND deleted = 0
            LIMIT 1
            """)
    Ticket findById(@Param("id") Long id);

    @Select("""
            SELECT COUNT(1)
            FROM ticket
            WHERE ticket_no = #{ticketNo}
              AND deleted = 0
            """)
    int countByTicketNo(@Param("ticketNo") String ticketNo);

    @Select("""
            SELECT COUNT(1)
            FROM ticket
            WHERE category_id = #{categoryId}
              AND deleted = 0
            """)
    int countByCategoryId(@Param("categoryId") Long categoryId);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM ticket t
            WHERE t.deleted = 0
              AND (
                #{admin} = TRUE
                OR t.creator_id = #{currentUserId}
                OR t.assignee_id = #{currentUserId}
                OR EXISTS (
                  SELECT 1
                  FROM team_member tm
                  WHERE tm.team_id = t.team_id
                    AND tm.user_id = #{currentUserId}
                    AND tm.deleted = 0
                )
              )
            <if test="'created'.equals(scope)">
              AND t.creator_id = #{currentUserId}
            </if>
            <if test="'assigned'.equals(scope)">
              AND t.assignee_id = #{currentUserId}
            </if>
            <if test="'watching'.equals(scope)">
              AND EXISTS (
                SELECT 1
                FROM ticket_watch tw
                WHERE tw.ticket_id = t.id
                  AND tw.user_id = #{currentUserId}
                  AND tw.deleted = 0
              )
            </if>
            <if test="ticketNo != null and ticketNo != ''">
              AND t.ticket_no LIKE CONCAT('%', #{ticketNo}, '%')
            </if>
            <if test="keyword != null and keyword != ''">
              AND (t.title LIKE CONCAT('%', #{keyword}, '%')
                   OR t.description LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="status != null and status != ''">
              AND t.status = #{status}
            </if>
            <if test="priority != null and priority != ''">
              AND t.priority = #{priority}
            </if>
            <if test="categoryId != null">
              AND t.category_id = #{categoryId}
            </if>
            <if test="creatorId != null">
              AND t.creator_id = #{creatorId}
            </if>
            <if test="assigneeId != null">
              AND t.assignee_id = #{assigneeId}
            </if>
            <if test="teamId != null">
              AND t.team_id = #{teamId}
            </if>
            <if test="overdue != null">
              AND t.overdue = #{overdue}
            </if>
            <if test="createdFrom != null">
              AND t.create_time &gt;= #{createdFrom}
            </if>
            <if test="createdTo != null">
              AND t.create_time &lt;= #{createdTo}
            </if>
            </script>
            """)
    long countSearch(@Param("scope") String scope,
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

    @Select("""
            <script>
            SELECT t.*
            FROM ticket t
            WHERE t.deleted = 0
              AND (
                #{admin} = TRUE
                OR t.creator_id = #{currentUserId}
                OR t.assignee_id = #{currentUserId}
                OR EXISTS (
                  SELECT 1
                  FROM team_member tm
                  WHERE tm.team_id = t.team_id
                    AND tm.user_id = #{currentUserId}
                    AND tm.deleted = 0
                )
              )
            <if test="'created'.equals(scope)">
              AND t.creator_id = #{currentUserId}
            </if>
            <if test="'assigned'.equals(scope)">
              AND t.assignee_id = #{currentUserId}
            </if>
            <if test="'watching'.equals(scope)">
              AND EXISTS (
                SELECT 1
                FROM ticket_watch tw
                WHERE tw.ticket_id = t.id
                  AND tw.user_id = #{currentUserId}
                  AND tw.deleted = 0
              )
            </if>
            <if test="ticketNo != null and ticketNo != ''">
              AND t.ticket_no LIKE CONCAT('%', #{ticketNo}, '%')
            </if>
            <if test="keyword != null and keyword != ''">
              AND (t.title LIKE CONCAT('%', #{keyword}, '%')
                   OR t.description LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="status != null and status != ''">
              AND t.status = #{status}
            </if>
            <if test="priority != null and priority != ''">
              AND t.priority = #{priority}
            </if>
            <if test="categoryId != null">
              AND t.category_id = #{categoryId}
            </if>
            <if test="creatorId != null">
              AND t.creator_id = #{creatorId}
            </if>
            <if test="assigneeId != null">
              AND t.assignee_id = #{assigneeId}
            </if>
            <if test="teamId != null">
              AND t.team_id = #{teamId}
            </if>
            <if test="overdue != null">
              AND t.overdue = #{overdue}
            </if>
            <if test="createdFrom != null">
              AND t.create_time &gt;= #{createdFrom}
            </if>
            <if test="createdTo != null">
              AND t.create_time &lt;= #{createdTo}
            </if>
            ORDER BY t.create_time DESC, t.id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
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
                        @Param("admin") boolean admin,
                        @Param("offset") long offset,
                        @Param("size") long size);

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
