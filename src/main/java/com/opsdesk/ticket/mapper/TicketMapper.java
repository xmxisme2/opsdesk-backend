package com.opsdesk.ticket.mapper;

import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.dashboard.mapper.DashboardAgentRankingRow;
import com.opsdesk.dashboard.mapper.DashboardDistributionRow;
import com.opsdesk.dashboard.mapper.DashboardTrendRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    List<Ticket> findOverdueCandidates(@Param("now") LocalDateTime now,
                                       @Param("limit") int limit);

    int markOverdue(@Param("id") Long id);

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

    long countWorkbenchByStatus(@Param("status") String status,
                                @Param("overdue") Integer overdue,
                                @Param("teamId") Long teamId,
                                @Param("currentUserId") Long currentUserId,
                                @Param("admin") boolean admin);

    long countWorkbenchCreated(@Param("currentUserId") Long currentUserId,
                               @Param("teamId") Long teamId);

    long countWorkbenchAssigned(@Param("currentUserId") Long currentUserId,
                                @Param("teamId") Long teamId);

    long countWorkbenchWatching(@Param("currentUserId") Long currentUserId,
                                @Param("teamId") Long teamId);

    List<Ticket> findWorkbenchLatest(@Param("teamId") Long teamId,
                                     @Param("currentUserId") Long currentUserId,
                                     @Param("admin") boolean admin,
                                     @Param("limit") int limit);

    long countDashboardCreated(@Param("dateFrom") LocalDateTime dateFrom,
                               @Param("dateTo") LocalDateTime dateTo,
                               @Param("teamId") Long teamId,
                               @Param("currentUserId") Long currentUserId,
                               @Param("admin") boolean admin);

    long countDashboardByStatusGroup(@Param("statuses") List<String> statuses,
                                     @Param("dateFrom") LocalDateTime dateFrom,
                                     @Param("dateTo") LocalDateTime dateTo,
                                     @Param("teamId") Long teamId,
                                     @Param("currentUserId") Long currentUserId,
                                     @Param("admin") boolean admin);

    long countDashboardOverdue(@Param("dateFrom") LocalDateTime dateFrom,
                               @Param("dateTo") LocalDateTime dateTo,
                               @Param("teamId") Long teamId,
                               @Param("currentUserId") Long currentUserId,
                               @Param("admin") boolean admin);

    Double avgDashboardProcessHours(@Param("dateFrom") LocalDateTime dateFrom,
                                    @Param("dateTo") LocalDateTime dateTo,
                                    @Param("teamId") Long teamId,
                                    @Param("currentUserId") Long currentUserId,
                                    @Param("admin") boolean admin);

    long countDashboardTotal(@Param("dateFrom") LocalDateTime dateFrom,
                             @Param("dateTo") LocalDateTime dateTo,
                             @Param("teamId") Long teamId,
                             @Param("currentUserId") Long currentUserId,
                             @Param("admin") boolean admin);

    List<DashboardTrendRow> findDashboardTrends(@Param("dateFrom") LocalDateTime dateFrom,
                                                @Param("dateTo") LocalDateTime dateTo,
                                                @Param("teamId") Long teamId,
                                                @Param("currentUserId") Long currentUserId,
                                                @Param("admin") boolean admin);

    List<DashboardDistributionRow> findDashboardDistribution(@Param("dimension") String dimension,
                                                            @Param("dateFrom") LocalDateTime dateFrom,
                                                            @Param("dateTo") LocalDateTime dateTo,
                                                            @Param("teamId") Long teamId,
                                                            @Param("currentUserId") Long currentUserId,
                                                            @Param("admin") boolean admin);

    List<DashboardAgentRankingRow> findDashboardAgentRanking(@Param("dateFrom") LocalDateTime dateFrom,
                                                             @Param("dateTo") LocalDateTime dateTo,
                                                             @Param("teamId") Long teamId,
                                                             @Param("currentUserId") Long currentUserId,
                                                             @Param("admin") boolean admin,
                                                             @Param("limit") int limit);

    List<Ticket> findDashboardOverdueTickets(@Param("teamId") Long teamId,
                                             @Param("priority") String priority,
                                             @Param("currentUserId") Long currentUserId,
                                             @Param("admin") boolean admin);

    int insert(Ticket ticket);

    int update(Ticket ticket);
}
