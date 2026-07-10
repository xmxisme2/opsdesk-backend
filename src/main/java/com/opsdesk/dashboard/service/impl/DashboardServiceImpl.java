package com.opsdesk.dashboard.service.impl;

import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.pagination.PageHelperPageResult;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.common.util.IdParser;
import com.opsdesk.dashboard.dto.DashboardAgentRankingRequest;
import com.opsdesk.dashboard.dto.DashboardDistributionRequest;
import com.opsdesk.dashboard.dto.DashboardOverdueTicketsRequest;
import com.opsdesk.dashboard.dto.DashboardRangeRequest;
import com.opsdesk.dashboard.dto.DashboardTrendRequest;
import com.opsdesk.dashboard.mapper.DashboardAgentRankingRow;
import com.opsdesk.dashboard.mapper.DashboardDistributionRow;
import com.opsdesk.dashboard.mapper.DashboardTrendRow;
import com.opsdesk.dashboard.service.DashboardService;
import com.opsdesk.dashboard.vo.DashboardAgentRankingItemVO;
import com.opsdesk.dashboard.vo.DashboardAgentRankingVO;
import com.opsdesk.dashboard.vo.DashboardDistributionItemVO;
import com.opsdesk.dashboard.vo.DashboardDistributionVO;
import com.opsdesk.dashboard.vo.DashboardSummaryVO;
import com.opsdesk.dashboard.vo.DashboardTrendPointVO;
import com.opsdesk.dashboard.vo.DashboardTrendVO;
import com.opsdesk.team.mapper.TeamMapper;
import com.opsdesk.ticket.converter.TicketConverter;
import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.ticket.mapper.TicketCategoryMapper;
import com.opsdesk.ticket.mapper.TicketMapper;
import com.opsdesk.ticket.vo.TicketListItemVO;
import com.opsdesk.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 数据看板服务实现。
 *
 * <p>首版采用实时聚合 SQL，后续数据量变大时可在不改变接口契约的前提下切换到预聚合表。</p>
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    /** 管理员角色编码：管理员可查看全局数据，外部请求不能伪造。 */
    private static final String ROLE_ADMIN = "ADMIN";

    /** 默认趋势范围：Figma 看板展示最近 30 天趋势。 */
    private static final String DEFAULT_RANGE = "30d";

    /** 允许的趋势范围：只开放固定窗口，避免任意字符串影响统计口径。 */
    private static final Set<String> ALLOWED_RANGES = Set.of("7d", "30d");

    /** 处理人排行默认数量：页面首屏展示前 5 名，避免列表过长。 */
    private static final int DEFAULT_RANKING_LIMIT = 5;

    /** 处理人排行最大数量：限制外部传入过大 limit 拖慢聚合。 */
    private static final int MAX_RANKING_LIMIT = 20;

    /** 允许的优先级筛选值：来自工单字典，外部只能传入这些编码。 */
    private static final Set<String> ALLOWED_PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final TicketMapper ticketMapper;
    private final TicketCategoryMapper ticketCategoryMapper;
    private final SysUserMapper sysUserMapper;
    private final TeamMapper teamMapper;
    private final TicketConverter ticketConverter;

    public DashboardServiceImpl(TicketMapper ticketMapper,
                                TicketCategoryMapper ticketCategoryMapper,
                                SysUserMapper sysUserMapper,
                                TeamMapper teamMapper,
                                TicketConverter ticketConverter) {
        this.ticketMapper = ticketMapper;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.sysUserMapper = sysUserMapper;
        this.teamMapper = teamMapper;
        this.ticketConverter = ticketConverter;
    }

    @Override
    public DashboardSummaryVO summary(DashboardRangeRequest request, CurrentUser currentUser) {
        DashboardScope scope = buildScope(request, currentUser);
        LocalDate today = LocalDate.now();
        long todayCreated = ticketMapper.countDashboardCreated(today.atStartOfDay(), today.plusDays(1).atStartOfDay(),
                scope.teamId(), scope.currentUserId(), scope.admin());
        long pendingCount = ticketMapper.countDashboardByStatusGroup(List.of("PENDING_ASSIGN", "PENDING_PROCESS"),
                scope.dateFrom(), scope.dateTo(), scope.teamId(), scope.currentUserId(), scope.admin());
        long processingCount = ticketMapper.countDashboardByStatusGroup(List.of("PROCESSING", "PENDING_CONFIRM"),
                scope.dateFrom(), scope.dateTo(), scope.teamId(), scope.currentUserId(), scope.admin());
        long overdueCount = ticketMapper.countDashboardOverdue(scope.dateFrom(), scope.dateTo(),
                scope.teamId(), scope.currentUserId(), scope.admin());
        Double avgHours = ticketMapper.avgDashboardProcessHours(scope.dateFrom(), scope.dateTo(),
                scope.teamId(), scope.currentUserId(), scope.admin());
        long total = ticketMapper.countDashboardTotal(scope.dateFrom(), scope.dateTo(),
                scope.teamId(), scope.currentUserId(), scope.admin());
        long completed = ticketMapper.countDashboardByStatusGroup(List.of("COMPLETED", "CLOSED"),
                scope.dateFrom(), scope.dateTo(), scope.teamId(), scope.currentUserId(), scope.admin());
        return new DashboardSummaryVO(
                todayCreated,
                pendingCount,
                processingCount,
                overdueCount,
                round(avgHours == null ? 0D : avgHours),
                total == 0 ? 0D : round(completed * 100D / total)
        );
    }

    @Override
    public DashboardTrendVO trends(DashboardTrendRequest request, CurrentUser currentUser) {
        DashboardScope scope = buildTrendScope(request, currentUser);
        List<DashboardTrendPointVO> points = ticketMapper.findDashboardTrends(
                        scope.dateFrom(), scope.dateTo(), scope.teamId(), scope.currentUserId(), scope.admin())
                .stream()
                .map(this::toTrendPoint)
                .toList();
        return new DashboardTrendVO(points);
    }

    @Override
    public DashboardDistributionVO distributions(DashboardDistributionRequest request, CurrentUser currentUser) {
        DashboardScope scope = buildScope(request, currentUser);
        String dimension = normalizeDimension(request == null ? null : request.getDimension());
        List<DashboardDistributionItemVO> items = ticketMapper.findDashboardDistribution(
                        dimension, scope.dateFrom(), scope.dateTo(), scope.teamId(), scope.currentUserId(), scope.admin())
                .stream()
                .map(this::toDistributionItem)
                .toList();
        return new DashboardDistributionVO(dimension, items);
    }

    @Override
    public DashboardAgentRankingVO agentRanking(DashboardAgentRankingRequest request, CurrentUser currentUser) {
        DashboardScope scope = buildScope(request, currentUser);
        int limit = normalizeLimit(request == null ? null : request.getLimit());
        List<DashboardAgentRankingItemVO> items = ticketMapper.findDashboardAgentRanking(
                        scope.dateFrom(), scope.dateTo(), scope.teamId(), scope.currentUserId(), scope.admin(), limit)
                .stream()
                .map(this::toRankingItem)
                .toList();
        return new DashboardAgentRankingVO(items);
    }

    @Override
    public PageResult<TicketListItemVO> overdueTickets(DashboardOverdueTicketsRequest request, CurrentUser currentUser) {
        DashboardOverdueTicketsRequest safeRequest = request == null ? new DashboardOverdueTicketsRequest() : request;
        DashboardScope scope = buildScope(safeRequest.getTeamId(), null, null, currentUser);
        String priority = normalizeOptionalPriority(safeRequest.getPriority());
        return PageHelperPageResult.selectPage(
                safeRequest,
                () -> ticketMapper.findDashboardOverdueTickets(scope.teamId(), priority, scope.currentUserId(), scope.admin()),
                this::toTicketListItem
        );
    }

    private DashboardScope buildTrendScope(DashboardTrendRequest request, CurrentUser currentUser) {
        DashboardScope explicitScope = buildScope(request, currentUser);
        if (explicitScope.dateFrom() != null || explicitScope.dateTo() != null) {
            return explicitScope;
        }
        String range = StringUtils.hasText(request == null ? null : request.getRange())
                ? request.getRange().trim().toLowerCase(Locale.ROOT)
                : DEFAULT_RANGE;
        if (!ALLOWED_RANGES.contains(range)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "趋势范围不正确");
        }
        int days = "7d".equals(range) ? 7 : 30;
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        return new DashboardScope(explicitScope.teamId(), end.minusDays(days), end,
                explicitScope.currentUserId(), explicitScope.admin());
    }

    private DashboardScope buildScope(DashboardRangeRequest request, CurrentUser currentUser) {
        return buildScope(
                request == null ? null : request.getTeamId(),
                request == null ? null : request.getDateFrom(),
                request == null ? null : request.getDateTo(),
                currentUser
        );
    }

    private DashboardScope buildScope(String teamId, String dateFrom, String dateTo, CurrentUser currentUser) {
        Long currentUserId = requireUserId(currentUser);
        return new DashboardScope(
                parseOptionalId(teamId, "团队ID"),
                parseOptionalDate(dateFrom, "开始日期", false),
                parseOptionalDate(dateTo, "结束日期", true),
                currentUserId,
                currentUser.getRoles().contains(ROLE_ADMIN)
        );
    }

    private DashboardTrendPointVO toTrendPoint(DashboardTrendRow row) {
        return new DashboardTrendPointVO(
                row.getStatDate(),
                safeLong(row.getCreatedCount()),
                safeLong(row.getCompletedCount()),
                safeLong(row.getOverdueCount())
        );
    }

    private DashboardDistributionItemVO toDistributionItem(DashboardDistributionRow row) {
        return new DashboardDistributionItemVO(row.getName(), safeLong(row.getValue()));
    }

    private DashboardAgentRankingItemVO toRankingItem(DashboardAgentRankingRow row) {
        return new DashboardAgentRankingItemVO(
                String.valueOf(row.getUserId()),
                row.getUserName(),
                safeLong(row.getCompletedCount()),
                round(row.getAvgProcessDuration() == null ? 0D : row.getAvgProcessDuration()),
                safeLong(row.getOverdueCount())
        );
    }

    private TicketListItemVO toTicketListItem(Ticket ticket) {
        return ticketConverter.toListItem(
                ticket,
                ticket.getCategoryId() == null ? null : ticketCategoryMapper.findById(ticket.getCategoryId()),
                ticket.getCreatorId() == null ? null : sysUserMapper.findById(ticket.getCreatorId()),
                ticket.getAssigneeId() == null ? null : sysUserMapper.findById(ticket.getAssigneeId()),
                ticket.getTeamId() == null ? null : teamMapper.findById(ticket.getTeamId())
        );
    }

    private String normalizeDimension(String dimension) {
        if (!StringUtils.hasText(dimension)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "统计维度不能为空");
        }
        String safeDimension = dimension.trim().toLowerCase(Locale.ROOT);
        if (Set.of("category", "priority", "status").contains(safeDimension)) {
            return safeDimension;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "统计维度不正确");
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_RANKING_LIMIT;
        }
        return Math.min(limit, MAX_RANKING_LIMIT);
    }

    private String normalizeOptionalPriority(String priority) {
        if (!StringUtils.hasText(priority)) {
            return null;
        }
        String safePriority = priority.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_PRIORITIES.contains(safePriority)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工单优先级不正确");
        }
        return safePriority;
    }

    private LocalDateTime parseOptionalDate(String value, String fieldName, boolean endOfDay) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value.trim(), DATE_FORMATTER);
            return endOfDay ? date.plusDays(1).atStartOfDay() : date.atStartOfDay();
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + "格式不正确");
        }
    }

    private Long parseOptionalId(String value, String fieldName) {
        return StringUtils.hasText(value) ? IdParser.parseRequired(value, fieldName) : null;
    }

    private Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }
        return currentUser.getUserId();
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private record DashboardScope(Long teamId,
                                  LocalDateTime dateFrom,
                                  LocalDateTime dateTo,
                                  Long currentUserId,
                                  boolean admin) {
    }
}
