package com.opsdesk.ticket.service.impl;

import com.opsdesk.attachment.converter.AttachmentConverter;
import com.opsdesk.attachment.entity.Attachment;
import com.opsdesk.attachment.mapper.AttachmentMapper;
import com.opsdesk.attachment.service.AttachmentResourceAccessService;
import com.opsdesk.attachment.service.AttachmentService;
import com.opsdesk.attachment.vo.AttachmentVO;
import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.pagination.PageHelperPageResult;
import com.opsdesk.common.pagination.PageQuery;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.common.util.IdParser;
import com.opsdesk.notification.service.NotificationService;
import com.opsdesk.team.entity.Team;
import com.opsdesk.team.mapper.TeamMapper;
import com.opsdesk.team.mapper.TeamMemberMapper;
import com.opsdesk.ticket.converter.TicketConverter;
import com.opsdesk.ticket.dto.TicketAssignRequest;
import com.opsdesk.ticket.dto.TicketCompleteRequest;
import com.opsdesk.ticket.dto.TicketCreateRequest;
import com.opsdesk.ticket.dto.TicketReasonRequest;
import com.opsdesk.ticket.dto.TicketSearchRequest;
import com.opsdesk.ticket.dto.TicketTransferRequest;
import com.opsdesk.ticket.dto.TicketUpdateRequest;
import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.ticket.entity.TicketCategory;
import com.opsdesk.ticket.entity.TicketOperationLog;
import com.opsdesk.ticket.entity.TicketWatch;
import com.opsdesk.ticket.enums.TicketAction;
import com.opsdesk.ticket.enums.TicketStatus;
import com.opsdesk.ticket.mapper.TicketCategoryMapper;
import com.opsdesk.ticket.mapper.TicketMapper;
import com.opsdesk.ticket.mapper.TicketOperationLogMapper;
import com.opsdesk.ticket.mapper.TicketWatchMapper;
import com.opsdesk.ticket.service.TicketNoGenerator;
import com.opsdesk.ticket.service.TicketService;
import com.opsdesk.ticket.service.TicketStateContext;
import com.opsdesk.ticket.service.TicketStateMachine;
import com.opsdesk.ticket.vo.TicketListItemVO;
import com.opsdesk.ticket.vo.TicketOperationLogVO;
import com.opsdesk.ticket.vo.TicketVO;
import com.opsdesk.ticket.vo.TicketWatchVO;
import com.opsdesk.ticket.vo.TicketTimelineVO;
import com.opsdesk.user.entity.SysUser;
import com.opsdesk.user.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.github.pagehelper.PageHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 工单主流程服务实现。
 *
 * <p>集中处理工单创建、草稿编辑、提交编号生成、状态机流转、资源范围校验和操作日志。</p>
 */
@Service
public class TicketServiceImpl implements TicketService {

    /** 管理员角色编码：可查看和操作全局工单，由认证上下文提供，外部请求体传入无效。 */
    private static final String ROLE_ADMIN = "ADMIN";

    /** 团队负责人角色编码：可操作所属团队工单，必须叠加 team_member 范围校验。 */
    private static final String ROLE_MANAGER = "MANAGER";

    /** 默认优先级：创建工单未传优先级时使用，允许外部传入的值必须在 ALLOWED_PRIORITIES 内。 */
    private static final String DEFAULT_PRIORITY = "MEDIUM";

    /** 允许的工单优先级编码：来自接口契约和数据库设计，外部只能传入这些枚举值。 */
    private static final Set<String> ALLOWED_PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");

    /** 草稿创建日志类型：写入 ticket_operation_log，表示工单对象已创建。 */
    private static final String OPERATION_TICKET_CREATE = "TICKET_CREATE";

    /** 草稿编辑日志类型：写入 ticket_operation_log，表示创建人修改了草稿信息。 */
    private static final String OPERATION_TICKET_UPDATE = "TICKET_UPDATE";

    /** 工单关注日志类型：记录用户关注工单，便于后续时间线聚合。 */
    private static final String OPERATION_TICKET_WATCH = "TICKET_WATCH";

    /** 取消关注日志类型：记录用户取消关注工单，便于后续时间线聚合。 */
    private static final String OPERATION_TICKET_UNWATCH = "TICKET_UNWATCH";

    /** 工单分派通知类型：写入 notification.type，外部请求不允许覆盖。 */
    private static final String NOTIFICATION_TICKET_ASSIGNED = "TICKET_ASSIGNED";

    /** 工单状态变更通知类型：写入 notification.type，用于接单、退回、完成等流转提醒。 */
    private static final String NOTIFICATION_TICKET_STATUS_CHANGED = "TICKET_STATUS_CHANGED";

    /** 工单关闭通知类型：写入 notification.type，用于最终关闭提醒。 */
    private static final String NOTIFICATION_TICKET_CLOSED = "TICKET_CLOSED";

    /** 标签最大数量：避免单个工单标签过多拖慢列表展示和筛选，外部传入超过会报参数错误。 */
    private static final int MAX_TAG_COUNT = 20;

    /** 单个标签最大长度：防止过长标签污染列表展示，外部传入超过会报参数错误。 */
    private static final int MAX_TAG_LENGTH = 30;

    /** 解决方案摘要最大长度：用于工单详情和知识草稿的根因/结论概述。 */
    private static final int MAX_RESOLUTION_SUMMARY_LENGTH = 1000;

    /** 单次同步导出上限：超过此数量需后续改用异步导出，避免占满 Web 请求内存。 */
    private static final int MAX_EXPORT_ROWS = 10_000;

    /** 导出审计操作类型：写入 audit_log，外部请求不能覆盖。 */
    private static final String AUDIT_OPERATION_TICKET_EXPORT = "TICKET_EXPORT";

    /** 工单审计业务类型：与审计日志数据字典保持一致。 */
    private static final String AUDIT_BIZ_TYPE_TICKET = "TICKET";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TicketMapper ticketMapper;
    private final TicketCategoryMapper ticketCategoryMapper;
    private final TicketOperationLogMapper ticketOperationLogMapper;
    private final TicketWatchMapper ticketWatchMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final SysUserMapper sysUserMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final TicketNoGenerator ticketNoGenerator;
    private final TicketStateMachine ticketStateMachine;
    private final TeamMapper teamMapper;
    private final TicketConverter ticketConverter;
    private final AttachmentMapper attachmentMapper;
    private final AttachmentConverter attachmentConverter;
    /** 附件服务：创建草稿、编辑草稿和提交完成时绑定当前用户的临时附件，避免出现孤立业务附件。 */
    private final AttachmentService attachmentService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public TicketServiceImpl(TicketMapper ticketMapper,
                             TicketCategoryMapper ticketCategoryMapper,
                             TicketOperationLogMapper ticketOperationLogMapper,
                             TicketWatchMapper ticketWatchMapper,
                             TeamMemberMapper teamMemberMapper,
                             SysUserMapper sysUserMapper,
                             SnowflakeIdGenerator idGenerator,
                             TicketNoGenerator ticketNoGenerator,
                             TicketStateMachine ticketStateMachine) {
        this(ticketMapper, ticketCategoryMapper, ticketOperationLogMapper, ticketWatchMapper, teamMemberMapper,
                sysUserMapper, idGenerator, ticketNoGenerator, ticketStateMachine, null, new TicketConverter(),
                null, new AttachmentConverter(), null, null, null);
    }

    public TicketServiceImpl(TicketMapper ticketMapper,
                             TicketCategoryMapper ticketCategoryMapper,
                             TicketOperationLogMapper ticketOperationLogMapper,
                             TicketWatchMapper ticketWatchMapper,
                             TeamMemberMapper teamMemberMapper,
                             SysUserMapper sysUserMapper,
                             SnowflakeIdGenerator idGenerator,
                             TicketNoGenerator ticketNoGenerator,
                             TicketStateMachine ticketStateMachine,
                             TeamMapper teamMapper,
                             TicketConverter ticketConverter,
                             AttachmentMapper attachmentMapper,
                             AttachmentConverter attachmentConverter) {
        this(ticketMapper, ticketCategoryMapper, ticketOperationLogMapper, ticketWatchMapper, teamMemberMapper,
                sysUserMapper, idGenerator, ticketNoGenerator, ticketStateMachine, teamMapper, ticketConverter,
                attachmentMapper, attachmentConverter, null, null, null);
    }

    @Autowired
    public TicketServiceImpl(TicketMapper ticketMapper,
                             TicketCategoryMapper ticketCategoryMapper,
                             TicketOperationLogMapper ticketOperationLogMapper,
                             TicketWatchMapper ticketWatchMapper,
                             TeamMemberMapper teamMemberMapper,
                             SysUserMapper sysUserMapper,
                             SnowflakeIdGenerator idGenerator,
                             TicketNoGenerator ticketNoGenerator,
                             TicketStateMachine ticketStateMachine,
                             TeamMapper teamMapper,
                             TicketConverter ticketConverter,
                             AttachmentMapper attachmentMapper,
                             AttachmentConverter attachmentConverter,
                             AttachmentService attachmentService,
                             NotificationService notificationService,
                             AuditLogService auditLogService) {
        this.ticketMapper = ticketMapper;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.ticketOperationLogMapper = ticketOperationLogMapper;
        this.ticketWatchMapper = ticketWatchMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.sysUserMapper = sysUserMapper;
        this.idGenerator = idGenerator;
        this.ticketNoGenerator = ticketNoGenerator;
        this.ticketStateMachine = ticketStateMachine;
        this.teamMapper = teamMapper;
        this.ticketConverter = ticketConverter;
        this.attachmentMapper = attachmentMapper;
        this.attachmentConverter = attachmentConverter;
        this.attachmentService = attachmentService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO create(TicketCreateRequest request, CurrentUser currentUser, String requestIp, String userAgent) {
        Long operatorId = requireUserId(currentUser);
        TicketCategory category = loadEnabledCategory(parseRequiredId(request.getCategoryId(), "工单分类ID"));
        LocalDateTime now = LocalDateTime.now();

        Ticket ticket = new Ticket();
        ticket.setId(idGenerator.nextId());
        ticket.setTitle(normalizeRequiredText(request.getTitle(), "工单标题"));
        ticket.setDescription(normalizeRequiredText(request.getDescription(), "问题描述"));
        ticket.setCategoryId(category.getId());
        ticket.setPriority(normalizePriority(request.getPriority()));
        ticket.setStatus(TicketStatus.DRAFT.name());
        ticket.setCreatorId(operatorId);
        ticket.setDueTime(resolveDueTime(request.getDueTime(), category, now));
        ticket.setOverdue(resolveOverdue(ticket.getDueTime(), now));
        // 新建工单尚未完成，解决结果固定标记为未验证，避免向非空数据库字段写入 null。
        ticket.setResolutionVerified(0);
        ticket.setTags(normalizeTags(request.getTags()));
        ticket.setCreateTime(now);
        ticket.setUpdateTime(now);
        ticket.setCreateBy(operatorId);
        ticket.setUpdateBy(operatorId);

        if (Boolean.TRUE.equals(request.getSubmitNow())) {
            submitNewTicket(ticket, category, currentUser);
        }

        ticketMapper.insert(ticket);
        bindTemporaryTicketAttachments(ticket.getId(), request.getAttachmentIds(), currentUser);
        recordLog(ticket, OPERATION_TICKET_CREATE, null, ticket.getStatus(), operatorId,
                "创建工单草稿", requestIp, userAgent);
        if (Boolean.TRUE.equals(request.getSubmitNow())) {
            recordLog(ticket, operationType(TicketAction.SUBMIT), TicketStatus.DRAFT.name(), ticket.getStatus(), operatorId,
                    "提交工单", requestIp, userAgent);
        }
        return assembleTicketVO(ticket, currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO updateDraft(String id,
                                TicketUpdateRequest request,
                                CurrentUser currentUser,
                                String requestIp,
                                String userAgent) {
        Long operatorId = requireUserId(currentUser);
        Ticket ticket = loadTicket(parseRequiredId(id, "工单ID"));
        ensureCreator(ticket, operatorId);
        ensureStatus(ticket, TicketStatus.DRAFT);
        TicketCategory category = loadEnabledCategory(parseRequiredId(request.getCategoryId(), "工单分类ID"));
        LocalDateTime now = LocalDateTime.now();
        String fromStatus = ticket.getStatus();

        ticket.setTitle(normalizeRequiredText(request.getTitle(), "工单标题"));
        ticket.setDescription(normalizeRequiredText(request.getDescription(), "问题描述"));
        ticket.setCategoryId(category.getId());
        ticket.setPriority(normalizePriority(request.getPriority()));
        ticket.setDueTime(resolveDueTime(request.getDueTime(), category, now));
        ticket.setOverdue(resolveOverdue(ticket.getDueTime(), now));
        ticket.setTags(normalizeTags(request.getTags()));
        ticket.setUpdateBy(operatorId);
        updateTicket(ticket);
        bindTemporaryTicketAttachments(ticket.getId(), request.getAttachmentIds(), currentUser);
        recordLog(ticket, OPERATION_TICKET_UPDATE, fromStatus, ticket.getStatus(), operatorId,
                "编辑工单草稿", requestIp, userAgent);
        return assembleTicketVO(ticket, currentUser);
    }

    @Override
    public PageResult<TicketListItemVO> search(TicketSearchRequest request, CurrentUser currentUser) {
        Long currentUserId = requireUserId(currentUser);
        TicketSearchRequest safeRequest = request == null ? new TicketSearchRequest() : request;
        SearchCondition condition = buildSearchCondition(safeRequest);
        boolean admin = hasRole(currentUser, ROLE_ADMIN);

        return PageHelperPageResult.selectPage(
                safeRequest,
                () -> ticketMapper.search(condition.scope(), condition.ticketNo(), condition.keyword(),
                        condition.status(), condition.priority(), condition.categoryId(), condition.creatorId(),
                        condition.assigneeId(), condition.teamId(), condition.overdue(), condition.createdFrom(),
                        condition.createdTo(), currentUserId, admin),
                this::assembleTicketListItemVO
        );
    }

    @Override
    public byte[] export(TicketSearchRequest request, CurrentUser currentUser, String requestIp, String userAgent) {
        Long operatorId = requireUserId(currentUser);
        if (!hasRole(currentUser, ROLE_MANAGER) && !hasRole(currentUser, ROLE_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅团队负责人或管理员可导出工单");
        }
        TicketSearchRequest safeRequest = request == null ? new TicketSearchRequest() : request;
        String format = trimToNull(safeRequest.getFormat());
        if (format != null && !"XLSX".equalsIgnoreCase(format)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前仅支持 XLSX 格式导出");
        }
        SearchCondition condition = buildSearchCondition(safeRequest);
        boolean admin = hasRole(currentUser, ROLE_ADMIN);
        // 额外查询一条用于识别超限；PageHelper 在 Mapper 查询阶段追加 LIMIT，避免整表加载到内存。
        PageHelper.startPage(1, MAX_EXPORT_ROWS + 1, false);
        try {
            List<Ticket> tickets = ticketMapper.search(condition.scope(), condition.ticketNo(), condition.keyword(),
                    condition.status(), condition.priority(), condition.categoryId(), condition.creatorId(),
                    condition.assigneeId(), condition.teamId(), condition.overdue(), condition.createdFrom(),
                    condition.createdTo(), operatorId, admin);
            if (tickets.size() > MAX_EXPORT_ROWS) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "导出数据超过10000条，请缩小筛选范围后重试");
            }
            List<TicketListItemVO> records = tickets.stream().map(this::assembleTicketListItemVO).toList();
            byte[] workbook = buildExportWorkbook(records);
            if (auditLogService != null) {
                auditLogService.record(operatorId, AUDIT_OPERATION_TICKET_EXPORT, AUDIT_BIZ_TYPE_TICKET, null,
                        "导出工单：" + records.size() + " 条", requestIp, userAgent);
            }
            return workbook;
        } finally {
            // Service 可被任务、测试等非 Web 调用复用，必须清理 ThreadLocal 分页上下文。
            PageHelper.clearPage();
        }
    }

    @Override
    public TicketVO detail(String id, CurrentUser currentUser) {
        Long currentUserId = requireUserId(currentUser);
        Ticket ticket = loadTicket(parseRequiredId(id, "工单ID"));
        ensureAccessible(ticket, currentUser);
        boolean watching = ticketWatchMapper.countActive(ticket.getId(), currentUserId) > 0;
        return assembleTicketVO(ticket, currentUser, watching);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO submit(String id, CurrentUser currentUser, String requestIp, String userAgent) {
        Long operatorId = requireUserId(currentUser);
        Ticket ticket = loadTicket(parseRequiredId(id, "工单ID"));
        String fromStatus = ticket.getStatus();
        TicketStatus targetStatus = ticketStateMachine.nextStatus(parseStatus(fromStatus), TicketAction.SUBMIT,
                buildStateContext(ticket, currentUser));
        TicketCategory category = loadEnabledCategory(ticket.getCategoryId());
        if (!StringUtils.hasText(ticket.getTicketNo())) {
            ticket.setTicketNo(ticketNoGenerator.nextNo());
        }
        if (ticket.getTeamId() == null) {
            ticket.setTeamId(category.getDefaultTeamId());
        }
        ticket.setStatus(targetStatus.name());
        ticket.setOverdue(resolveOverdue(ticket.getDueTime(), LocalDateTime.now()));
        ticket.setUpdateBy(operatorId);
        updateTicket(ticket);
        recordLog(ticket, operationType(TicketAction.SUBMIT), fromStatus, ticket.getStatus(), operatorId,
                "提交工单", requestIp, userAgent);
        notifyTicketStatusChanged(ticket, operatorId, "工单已提交");
        return assembleTicketVO(ticket, currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO assign(String id,
                           TicketAssignRequest request,
                           CurrentUser currentUser,
                           String requestIp,
                           String userAgent) {
        Long operatorId = requireUserId(currentUser);
        Ticket ticket = loadTicket(parseRequiredId(id, "工单ID"));
        Long teamId = parseOptionalId(request == null ? null : request.getTeamId(), "处理团队ID");
        Long assigneeId = parseOptionalId(request == null ? null : request.getAssigneeId(), "处理人ID");
        if (teamId == null && assigneeId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "处理团队和处理人至少需要填写一个");
        }
        Long finalTeamId = teamId == null ? ticket.getTeamId() : teamId;
        ensureAssignmentScope(currentUser, ticket.getTeamId(), finalTeamId);
        ensureAssigneeInTeam(finalTeamId, assigneeId);
        String fromStatus = ticket.getStatus();
        TicketStatus targetStatus = ticketStateMachine.nextStatus(parseStatus(fromStatus), TicketAction.ASSIGN,
                buildStateContext(ticket, currentUser));
        ticket.setTeamId(finalTeamId);
        ticket.setAssigneeId(assigneeId);
        ticket.setStatus(targetStatus.name());
        ticket.setUpdateBy(operatorId);
        updateTicket(ticket);
        recordLog(ticket, operationType(TicketAction.ASSIGN), fromStatus, ticket.getStatus(), operatorId,
                contentOrDefault(request == null ? null : request.getReason(), "分派工单"), requestIp, userAgent);
        notifyTicketAssignment(ticket, operatorId);
        return assembleTicketVO(ticket, currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO reject(String id,
                           TicketReasonRequest request,
                           CurrentUser currentUser,
                           String requestIp,
                           String userAgent) {
        Long operatorId = requireUserId(currentUser);
        Ticket ticket = loadTicket(parseRequiredId(id, "工单ID"));
        ensureReason(request == null ? null : request.getReason(), "驳回原因不能为空");
        if (parseStatus(ticket.getStatus()) == TicketStatus.PENDING_ASSIGN) {
            ensureManagerScope(currentUser, ticket.getTeamId());
        }
        String fromStatus = ticket.getStatus();
        TicketStatus targetStatus = ticketStateMachine.nextStatus(parseStatus(fromStatus), TicketAction.REJECT,
                buildStateContext(ticket, currentUser));
        ticket.setStatus(targetStatus.name());
        ticket.setAssigneeId(null);
        ticket.setUpdateBy(operatorId);
        updateTicket(ticket);
        recordLog(ticket, operationType(TicketAction.REJECT), fromStatus, ticket.getStatus(), operatorId,
                request.getReason().trim(), requestIp, userAgent);
        notifyTicketStatusChanged(ticket, operatorId, targetStatus == TicketStatus.PENDING_ASSIGN
                ? "工单已退回团队负责人"
                : "工单已驳回");
        return assembleTicketVO(ticket, currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO accept(String id, CurrentUser currentUser, String requestIp, String userAgent) {
        Long operatorId = requireUserId(currentUser);
        Ticket ticket = loadTicket(parseRequiredId(id, "工单ID"));
        String fromStatus = ticket.getStatus();
        TicketStatus targetStatus = ticketStateMachine.nextStatus(parseStatus(fromStatus), TicketAction.ACCEPT,
                buildStateContext(ticket, currentUser));
        ticket.setAssigneeId(operatorId);
        ticket.setStatus(targetStatus.name());
        ticket.setUpdateBy(operatorId);
        updateTicket(ticket);
        recordLog(ticket, operationType(TicketAction.ACCEPT), fromStatus, ticket.getStatus(), operatorId,
                "接单处理", requestIp, userAgent);
        notifyTicketStatusChanged(ticket, operatorId, "工单已接单");
        return assembleTicketVO(ticket, currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO transfer(String id,
                             TicketTransferRequest request,
                             CurrentUser currentUser,
                             String requestIp,
                             String userAgent) {
        Long operatorId = requireUserId(currentUser);
        Ticket ticket = loadTicket(parseRequiredId(id, "工单ID"));
        Long targetTeamId = parseOptionalId(request == null ? null : request.getTargetTeamId(), "目标团队ID");
        Long targetAssigneeId = parseOptionalId(request == null ? null : request.getTargetAssigneeId(), "目标处理人ID");
        if (targetTeamId == null && targetAssigneeId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "目标团队和目标处理人至少需要填写一个");
        }
        ensureReason(request == null ? null : request.getReason(), "转派原因不能为空");
        Long finalTeamId = targetTeamId == null ? ticket.getTeamId() : targetTeamId;
        ensureAssignmentScope(currentUser, ticket.getTeamId(), finalTeamId);
        ensureAssigneeInTeam(finalTeamId, targetAssigneeId);
        String fromStatus = ticket.getStatus();
        TicketStatus targetStatus = ticketStateMachine.nextStatus(parseStatus(fromStatus), TicketAction.TRANSFER,
                buildStateContext(ticket, currentUser));
        ticket.setTeamId(finalTeamId);
        ticket.setAssigneeId(targetAssigneeId);
        ticket.setStatus(targetStatus.name());
        ticket.setUpdateBy(operatorId);
        updateTicket(ticket);
        recordLog(ticket, operationType(TicketAction.TRANSFER), fromStatus, ticket.getStatus(), operatorId,
                request.getReason().trim(), requestIp, userAgent);
        notifyTicketAssignment(ticket, operatorId);
        return assembleTicketVO(ticket, currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO complete(String id,
                             TicketCompleteRequest request,
                             CurrentUser currentUser,
                             String requestIp,
                             String userAgent) {
        Long operatorId = requireUserId(currentUser);
        Ticket ticket = loadTicket(parseRequiredId(id, "工单ID"));
        String fromStatus = ticket.getStatus();
        TicketStatus targetStatus = ticketStateMachine.nextStatus(parseStatus(fromStatus), TicketAction.COMPLETE,
                buildStateContext(ticket, currentUser));
        CompletionResolution resolution = resolveCompletionResolution(request);
        ticket.setStatus(targetStatus.name());
        ticket.setResolutionSummary(resolution.summary());
        ticket.setResolutionSteps(resolution.steps());
        ticket.setResolutionVerified(resolution.verified() ? 1 : 0);
        ticket.setUpdateBy(operatorId);
        updateTicket(ticket);
        bindTemporaryTicketAttachments(ticket.getId(), request == null ? null : request.getAttachmentIds(), currentUser);
        recordLog(ticket, operationType(TicketAction.COMPLETE), fromStatus, ticket.getStatus(), operatorId,
                "解决方案：" + resolution.summary() + (resolution.verified() ? "；已验证" : "；待验证"), requestIp, userAgent);
        notifyTicketStatusChanged(ticket, operatorId, "工单已提交完成");
        return assembleTicketVO(ticket, currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO confirm(String id,
                            TicketReasonRequest request,
                            CurrentUser currentUser,
                            String requestIp,
                            String userAgent) {
        Long operatorId = requireUserId(currentUser);
        Ticket ticket = loadTicket(parseRequiredId(id, "工单ID"));
        String fromStatus = ticket.getStatus();
        TicketStatus targetStatus = ticketStateMachine.nextStatus(parseStatus(fromStatus), TicketAction.CONFIRM,
                buildStateContext(ticket, currentUser));
        ticket.setStatus(targetStatus.name());
        ticket.setCompletedTime(LocalDateTime.now());
        ticket.setUpdateBy(operatorId);
        updateTicket(ticket);
        recordLog(ticket, operationType(TicketAction.CONFIRM), fromStatus, ticket.getStatus(), operatorId,
                contentOrDefault(request == null ? null : request.getComment(), "确认完成"), requestIp, userAgent);
        notifyTicketStatusChanged(ticket, operatorId, "工单已确认完成");
        return assembleTicketVO(ticket, currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO reopen(String id,
                           TicketReasonRequest request,
                           CurrentUser currentUser,
                           String requestIp,
                           String userAgent) {
        Long operatorId = requireUserId(currentUser);
        Ticket ticket = loadTicket(parseRequiredId(id, "工单ID"));
        ensureReason(request == null ? null : request.getReason(), "重开原因不能为空");
        String fromStatus = ticket.getStatus();
        TicketStatus targetStatus = ticketStateMachine.nextStatus(parseStatus(fromStatus), TicketAction.REOPEN,
                buildStateContext(ticket, currentUser));
        ticket.setStatus(targetStatus.name());
        ticket.setCompletedTime(null);
        ticket.setUpdateBy(operatorId);
        updateTicket(ticket);
        recordLog(ticket, operationType(TicketAction.REOPEN), fromStatus, ticket.getStatus(), operatorId,
                request.getReason().trim(), requestIp, userAgent);
        notifyTicketStatusChanged(ticket, operatorId, "工单已重开");
        return assembleTicketVO(ticket, currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO close(String id,
                          TicketReasonRequest request,
                          CurrentUser currentUser,
                          String requestIp,
                          String userAgent) {
        Long operatorId = requireUserId(currentUser);
        Ticket ticket = loadTicket(parseRequiredId(id, "工单ID"));
        ensureManagerScopeForClose(currentUser, ticket);
        String fromStatus = ticket.getStatus();
        TicketStatus targetStatus = ticketStateMachine.nextStatus(parseStatus(fromStatus), TicketAction.CLOSE,
                buildStateContext(ticket, currentUser));
        ticket.setStatus(targetStatus.name());
        ticket.setClosedTime(LocalDateTime.now());
        ticket.setUpdateBy(operatorId);
        updateTicket(ticket);
        recordLog(ticket, operationType(TicketAction.CLOSE), fromStatus, ticket.getStatus(), operatorId,
                contentOrDefault(request == null ? null : request.getReason(), "关闭工单"), requestIp, userAgent);
        notifyTicketClosed(ticket, operatorId);
        return assembleTicketVO(ticket, currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO cancel(String id,
                           TicketReasonRequest request,
                           CurrentUser currentUser,
                           String requestIp,
                           String userAgent) {
        Long operatorId = requireUserId(currentUser);
        Ticket ticket = loadTicket(parseRequiredId(id, "工单ID"));
        ensureReason(request == null ? null : request.getReason(), "取消原因不能为空");
        String fromStatus = ticket.getStatus();
        TicketStatus targetStatus = ticketStateMachine.nextStatus(parseStatus(fromStatus), TicketAction.CANCEL,
                buildStateContext(ticket, currentUser));
        ticket.setStatus(targetStatus.name());
        ticket.setUpdateBy(operatorId);
        updateTicket(ticket);
        recordLog(ticket, operationType(TicketAction.CANCEL), fromStatus, ticket.getStatus(), operatorId,
                request.getReason().trim(), requestIp, userAgent);
        notifyTicketStatusChanged(ticket, operatorId, "工单已取消");
        return assembleTicketVO(ticket, currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketWatchVO watch(String id, CurrentUser currentUser, String requestIp, String userAgent) {
        Long operatorId = requireUserId(currentUser);
        Ticket ticket = loadTicket(parseRequiredId(id, "工单ID"));
        ensureAccessible(ticket, currentUser);
        if (ticketWatchMapper.countActive(ticket.getId(), operatorId) == 0) {
            TicketWatch watch = new TicketWatch();
            watch.setId(idGenerator.nextId());
            watch.setTicketId(ticket.getId());
            watch.setUserId(operatorId);
            watch.setCreateBy(operatorId);
            watch.setUpdateBy(operatorId);
            if (ticketWatchMapper.restoreDeleted(watch) == 0) {
                ticketWatchMapper.insert(watch);
            }
            recordLog(ticket, OPERATION_TICKET_WATCH, ticket.getStatus(), ticket.getStatus(), operatorId,
                    "关注工单", requestIp, userAgent);
        }
        return new TicketWatchVO(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketWatchVO unwatch(String id, CurrentUser currentUser, String requestIp, String userAgent) {
        Long operatorId = requireUserId(currentUser);
        Ticket ticket = loadTicket(parseRequiredId(id, "工单ID"));
        ensureAccessible(ticket, currentUser);
        if (ticketWatchMapper.logicalDelete(ticket.getId(), operatorId, operatorId) > 0) {
            recordLog(ticket, OPERATION_TICKET_UNWATCH, ticket.getStatus(), ticket.getStatus(), operatorId,
                    "取消关注工单", requestIp, userAgent);
        }
        return new TicketWatchVO(false);
    }

    @Override
    public PageResult<TicketOperationLogVO> searchOperationLogs(String id, PageQuery request, CurrentUser currentUser) {
        Ticket ticket = loadTicket(parseRequiredId(id, "工单ID"));
        ensureAccessible(ticket, currentUser);
        PageQuery safeRequest = request == null ? new PageQuery() : request;
        return PageHelperPageResult.selectPage(
                safeRequest,
                () -> ticketOperationLogMapper.searchByTicketId(ticket.getId()),
                log -> ticketConverter.toOperationLogVO(log, findUser(log.getOperatorId()))
        );
    }

    private void submitNewTicket(Ticket ticket, TicketCategory category, CurrentUser currentUser) {
        TicketStatus targetStatus = ticketStateMachine.nextStatus(TicketStatus.DRAFT, TicketAction.SUBMIT,
                buildStateContext(ticket, currentUser));
        ticket.setTicketNo(ticketNoGenerator.nextNo());
        ticket.setTeamId(category.getDefaultTeamId());
        ticket.setStatus(targetStatus.name());
    }

    private SearchCondition buildSearchCondition(TicketSearchRequest request) {
        return new SearchCondition(
                normalizeScope(request.getScope()),
                request.normalizedTicketNo(),
                request.normalizedKeyword(),
                normalizeOptionalStatus(request.getStatus()),
                normalizeOptionalPriority(request.getPriority()),
                parseOptionalId(request.getCategoryId(), "工单分类ID"),
                parseOptionalId(request.getCreatorId(), "创建人ID"),
                parseOptionalId(request.getAssigneeId(), "处理人ID"),
                parseOptionalId(request.getTeamId(), "处理团队ID"),
                request.getOverdue() == null ? null : (request.getOverdue() ? 1 : 0),
                parseOptionalDateTime(request.getCreatedFrom(), "创建开始时间"),
                parseOptionalDateTime(request.getCreatedTo(), "创建结束时间")
        );
    }

    @Override
    public TicketTimelineVO timeline(String id, CurrentUser currentUser) {
        Ticket ticket = loadTicket(parseRequiredId(id, "工单ID"));
        ensureAccessible(ticket, currentUser);
        Long userId = requireUserId(currentUser);
        boolean includeInternal = hasRole(currentUser, "AGENT")
                || hasRole(currentUser, ROLE_MANAGER)
                || hasRole(currentUser, ROLE_ADMIN)
                || sameUser(userId, ticket.getAssigneeId())
                || isTeamMember(ticket.getTeamId(), userId);
        return new TicketTimelineVO(ticketOperationLogMapper.searchTimeline(ticket.getId(), includeInternal));
    }

    /**
     * 生成工单 XLSX 工作簿。单元格统一按文本写入，规避标题等用户输入以公式方式被 Excel 执行。
     */
    private byte[] buildExportWorkbook(List<TicketListItemVO> records) {
        String[] headers = {"工单编号", "标题", "分类", "优先级", "状态", "提交人", "处理人", "处理团队", "SLA 截止时间", "SLA 状态", "创建时间", "更新时间"};
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.setCompressTempFiles(true);
            SXSSFSheet sheet = workbook.createSheet("工单列表");
            sheet.createFreezePane(0, 1);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            var headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(headers[index]);
                cell.setCellStyle(headerStyle);
            }
            int[] widths = {20, 36, 18, 12, 16, 16, 16, 18, 20, 12, 20, 20};
            for (int index = 0; index < widths.length; index++) {
                sheet.setColumnWidth(index, widths[index] * 256);
            }
            for (int index = 0; index < records.size(); index++) {
                TicketListItemVO ticket = records.get(index);
                Row row = sheet.createRow(index + 1);
                String[] values = {
                        ticket.ticketNo(), ticket.title(), ticket.categoryName(), priorityLabel(ticket.priority()),
                        statusLabel(ticket.status()), ticket.creatorName(), ticket.assigneeName(), ticket.teamName(),
                        ticket.dueTime(), Boolean.TRUE.equals(ticket.overdue()) ? "已超时" : "未超时",
                        ticket.createdAt(), ticket.updatedAt()
                };
                for (int column = 0; column < values.length; column++) {
                    row.createCell(column).setCellValue(safeExcelText(values[column]));
                }
            }
            workbook.write(output);
            workbook.dispose();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成工单导出文件失败");
        }
    }

    /** Excel 会将等号等开头的值识别为公式，导出用户输入前需转义为纯文本。 */
    private String safeExcelText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String text = value.trim();
        return text.startsWith("=") || text.startsWith("+") || text.startsWith("-") || text.startsWith("@")
                ? "'" + text : text;
    }

    /** 工单优先级中文映射：仅展示使用，数据层仍保持稳定的英文枚举编码。 */
    private String priorityLabel(String priority) {
        return switch (priority == null ? "" : priority) {
            case "LOW" -> "低";
            case "MEDIUM" -> "中";
            case "HIGH" -> "高";
            case "URGENT" -> "紧急";
            default -> priority;
        };
    }

    /** 工单状态中文映射：导出面向业务人员，不直接暴露内部状态编码。 */
    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "DRAFT" -> "草稿";
            case "PENDING_ASSIGN" -> "待分派";
            case "PENDING_PROCESS" -> "待处理";
            case "PROCESSING" -> "处理中";
            case "PENDING_CONFIRM" -> "待确认";
            case "COMPLETED" -> "已完成";
            case "CLOSED" -> "已关闭";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    private TicketStateContext buildStateContext(Ticket ticket, CurrentUser currentUser) {
        boolean teamMember = ticket.getTeamId() != null
                && teamMemberMapper.countActive(ticket.getTeamId(), requireUserId(currentUser)) > 0;
        return TicketStateContext.of(
                currentUser.getUserId(),
                ticket.getCreatorId(),
                ticket.getAssigneeId(),
                Set.copyOf(currentUser.getRoles()),
                teamMember
        );
    }

    private TicketVO assembleTicketVO(Ticket ticket, CurrentUser currentUser) {
        boolean watching = ticketWatchMapper.countActive(ticket.getId(), requireUserId(currentUser)) > 0;
        return assembleTicketVO(ticket, currentUser, watching);
    }

    private TicketVO assembleTicketVO(Ticket ticket, CurrentUser currentUser, boolean watching) {
        return ticketConverter.toVO(
                ticket,
                findCategory(ticket.getCategoryId()),
                findUser(ticket.getCreatorId()),
                findUser(ticket.getAssigneeId()),
                findTeam(ticket.getTeamId()),
                watching,
                findTicketAttachments(ticket.getId()),
                resolveAvailableActions(ticket, currentUser)
        );
    }

    private List<String> resolveAvailableActions(Ticket ticket, CurrentUser currentUser) {
        if (ticket == null || currentUser == null || currentUser.getUserId() == null) {
            return List.of();
        }
        TicketStatus currentStatus = parseStatus(ticket.getStatus());
        TicketStateContext context = buildStateContext(ticket, currentUser);
        return candidateActions(currentStatus).stream()
                .filter(action -> canExecute(currentStatus, action, context))
                .map(this::actionCode)
                .toList();
    }

    /**
     * 可用动作只列出当前状态可能出现的详情页按钮，具体权限继续复用状态机校验。
     */
    private List<TicketAction> candidateActions(TicketStatus currentStatus) {
        return switch (currentStatus) {
            case DRAFT -> List.of(TicketAction.SUBMIT, TicketAction.CANCEL);
            case PENDING_ASSIGN -> List.of(TicketAction.ASSIGN, TicketAction.REJECT, TicketAction.CANCEL);
            case PENDING_PROCESS -> List.of(TicketAction.ACCEPT, TicketAction.REJECT, TicketAction.TRANSFER);
            case PROCESSING -> List.of(TicketAction.TRANSFER, TicketAction.REJECT, TicketAction.COMPLETE);
            case PENDING_CONFIRM -> List.of(TicketAction.CONFIRM, TicketAction.REOPEN);
            case COMPLETED -> List.of(TicketAction.CLOSE);
            case CLOSED, CANCELLED -> List.of();
        };
    }

    private boolean canExecute(TicketStatus currentStatus, TicketAction action, TicketStateContext context) {
        try {
            ticketStateMachine.nextStatus(currentStatus, action, context);
            return true;
        } catch (BusinessException exception) {
            return false;
        }
    }

    private String actionCode(TicketAction action) {
        return action.name().toLowerCase(Locale.ROOT);
    }

    private TicketListItemVO assembleTicketListItemVO(Ticket ticket) {
        return ticketConverter.toListItem(
                ticket,
                findCategory(ticket.getCategoryId()),
                findUser(ticket.getCreatorId()),
                findUser(ticket.getAssigneeId()),
                findTeam(ticket.getTeamId())
        );
    }

    private void recordLog(Ticket ticket,
                           String operationType,
                           String fromStatus,
                           String toStatus,
                           Long operatorId,
                           String content,
                           String requestIp,
                           String userAgent) {
        TicketOperationLog log = new TicketOperationLog();
        log.setId(idGenerator.nextId());
        log.setTicketId(ticket.getId());
        log.setOperationType(operationType);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(operatorId);
        log.setContent(trimToNull(content));
        log.setRequestIp(trimToNull(requestIp));
        log.setUserAgent(trimToNull(userAgent));
        log.setCreateBy(operatorId);
        log.setUpdateBy(operatorId);
        ticketOperationLogMapper.insert(log);
    }

    /**
     * 工单分派通知：有具体处理人时只通知处理人；仅分派到团队时通知团队负责人。
     */
    private void notifyTicketAssignment(Ticket ticket, Long operatorId) {
        notifyTicketReceivers(ticket, operatorId, NOTIFICATION_TICKET_ASSIGNED, false);
    }

    /**
     * 工单状态通知：通知创建人、当前处理人；没有处理人时补充通知团队负责人。
     */
    private void notifyTicketStatusChanged(Ticket ticket, Long operatorId, String title) {
        notifyTicketReceivers(ticket, operatorId, NOTIFICATION_TICKET_STATUS_CHANGED, true);
    }

    private void notifyTicketClosed(Ticket ticket, Long operatorId) {
        notifyTicketReceivers(ticket, operatorId, NOTIFICATION_TICKET_CLOSED, true);
    }

    private void notifyTicketReceivers(Ticket ticket,
                                       Long operatorId,
                                       String type,
                                       boolean includeCreator) {
        if (notificationService == null || ticket == null) {
            return;
        }
        LinkedHashSet<Long> receiverIds = new LinkedHashSet<>();
        if (includeCreator) {
            receiverIds.add(ticket.getCreatorId());
        }
        if (ticket.getAssigneeId() != null) {
            receiverIds.add(ticket.getAssigneeId());
        } else if (ticket.getTeamId() != null) {
            List<Long> leaderIds = teamMemberMapper.findLeaderIdsByTeamId(ticket.getTeamId());
            if (leaderIds != null) {
                receiverIds.addAll(leaderIds);
            }
        }
        receiverIds.remove(null);
        receiverIds.remove(operatorId);
        Map<String, String> variables = Map.of(
                "ticketNo", displayTicketNo(ticket),
                "assignee", userDisplayName(ticket.getAssigneeId(), "待分派"),
                "teamName", teamDisplayName(ticket.getTeamId(), "未分配团队"),
                "operatorName", userDisplayName(operatorId, "系统"),
                "status", statusDisplayName(ticket.getStatus())
        );
        receiverIds.forEach(receiverId -> notificationService.createTicketNotification(receiverId, type, variables, ticket.getId(), operatorId));
    }

    /** 通知变量中的用户名称优先显示昵称，其次用户名，系统任务或未知用户使用明确兜底文案。 */
    private String userDisplayName(Long userId, String fallback) {
        if (userId == null) return fallback;
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) return fallback;
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    /** 通知变量中的处理组名称仅来自有效团队，团队不存在或未分派时使用明确兜底文案。 */
    private String teamDisplayName(Long teamId, String fallback) {
        Team team = findTeam(teamId);
        return team == null || !StringUtils.hasText(team.getName()) ? fallback : team.getName();
    }

    /** 将状态编码转换为通知用户可理解的中文状态名称。 */
    private String statusDisplayName(String status) {
        if (status == null) return "未知状态";
        return switch (status) {
            case "DRAFT" -> "草稿"; case "PENDING_ASSIGN" -> "待分派"; case "PENDING_PROCESS" -> "待处理";
            case "PROCESSING" -> "处理中"; case "PENDING_CONFIRM" -> "待确认"; case "COMPLETED" -> "已完成";
            case "CLOSED" -> "已关闭"; case "CANCELLED" -> "已取消"; default -> status;
        };
    }

    private String displayTicketNo(Ticket ticket) {
        if (ticket == null) {
            return "-";
        }
        return StringUtils.hasText(ticket.getTicketNo()) ? ticket.getTicketNo() : String.valueOf(ticket.getId());
    }

    private Ticket loadTicket(Long ticketId) {
        Ticket ticket = ticketMapper.findById(ticketId);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        }
        return ticket;
    }

    private TicketCategory loadEnabledCategory(Long categoryId) {
        // 工单写事务先锁定分类行，保证引用写入前分类不会被并发逻辑删除。
        TicketCategory category = ticketCategoryMapper.findByIdForUpdate(categoryId);
        if (category == null || category.getEnabled() == null || category.getEnabled() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工单分类不存在或未启用");
        }
        return category;
    }

    private TicketCategory findCategory(Long categoryId) {
        return categoryId == null ? null : ticketCategoryMapper.findById(categoryId);
    }

    private SysUser findUser(Long userId) {
        return userId == null ? null : sysUserMapper.findById(userId);
    }

    private Team findTeam(Long teamId) {
        return teamId == null || teamMapper == null ? null : teamMapper.findById(teamId);
    }

    private List<AttachmentVO> findTicketAttachments(Long ticketId) {
        if (ticketId == null || attachmentMapper == null || attachmentConverter == null) {
            return List.of();
        }
        List<Attachment> attachments = attachmentMapper.findByBiz(AttachmentResourceAccessService.BIZ_TYPE_TICKET, ticketId);
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments.stream()
                .map(attachmentConverter::toVO)
                .toList();
    }

    /**
     * 仅绑定本次请求显式携带的临时附件；附件服务会复核上传人、临时状态及工单访问权限。
     */
    private void bindTemporaryTicketAttachments(Long ticketId, List<String> attachmentIds, CurrentUser currentUser) {
        if (attachmentService == null || attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        attachmentService.bindTemporaryAttachments(AttachmentResourceAccessService.BIZ_TYPE_TICKET,
                ticketId, attachmentIds, currentUser);
    }

    private void updateTicket(Ticket ticket) {
        if (ticketMapper.update(ticket) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        }
    }

    private void ensureAccessible(Ticket ticket, CurrentUser currentUser) {
        Long operatorId = requireUserId(currentUser);
        if (hasRole(currentUser, ROLE_ADMIN)
                || sameUser(operatorId, ticket.getCreatorId())
                || sameUser(operatorId, ticket.getAssigneeId())
                || isTeamMember(ticket.getTeamId(), operatorId)) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无权访问该工单");
    }

    private void ensureCreator(Ticket ticket, Long operatorId) {
        if (!sameUser(ticket.getCreatorId(), operatorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅创建人可编辑该工单");
        }
    }

    private void ensureStatus(Ticket ticket, TicketStatus expectedStatus) {
        if (parseStatus(ticket.getStatus()) != expectedStatus) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "当前状态不允许编辑");
        }
    }

    private void ensureManagerScope(CurrentUser currentUser, Long teamId) {
        if (hasRole(currentUser, ROLE_ADMIN) || !hasRole(currentUser, ROLE_MANAGER)) {
            return;
        }
        if (teamId == null || !isTeamLeader(teamId, currentUser.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "负责人只能操作所属团队工单");
        }
    }

    private void ensureAssignmentScope(CurrentUser currentUser, Long currentTeamId, Long targetTeamId) {
        ensureManagerScope(currentUser, currentTeamId == null ? targetTeamId : currentTeamId);
        if (targetTeamId != null && currentTeamId != null && !targetTeamId.equals(currentTeamId)) {
            ensureManagerScope(currentUser, targetTeamId);
        }
    }

    private void ensureAssigneeInTeam(Long teamId, Long assigneeId) {
        if (assigneeId == null) {
            return;
        }
        if (teamId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "指定处理人时必须先选择处理团队");
        }
        if (!isTeamMember(teamId, assigneeId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "处理人必须属于所选团队");
        }
    }

    private void ensureManagerScopeForClose(CurrentUser currentUser, Ticket ticket) {
        if (hasRole(currentUser, ROLE_MANAGER) && !sameUser(currentUser.getUserId(), ticket.getCreatorId())) {
            ensureManagerScope(currentUser, ticket.getTeamId());
        }
    }

    private boolean isTeamMember(Long teamId, Long userId) {
        return teamId != null && userId != null && teamMemberMapper.countActive(teamId, userId) > 0;
    }

    private boolean isTeamLeader(Long teamId, Long userId) {
        return teamId != null && userId != null && teamMemberMapper.countLeader(teamId, userId) > 0;
    }

    private boolean hasRole(CurrentUser currentUser, String role) {
        return currentUser != null && currentUser.getRoles().contains(role);
    }

    private boolean sameUser(Long left, Long right) {
        return left != null && left.equals(right);
    }

    private Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }
        return currentUser.getUserId();
    }

    private Long parseRequiredId(String value, String fieldName) {
        return IdParser.parseRequired(value, fieldName);
    }

    private Long parseOptionalId(String value, String fieldName) {
        return StringUtils.hasText(value) ? IdParser.parseRequired(value, fieldName) : null;
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + "不能为空");
        }
        return value.trim();
    }

    private String normalizePriority(String priority) {
        String safePriority = StringUtils.hasText(priority) ? priority.trim().toUpperCase() : DEFAULT_PRIORITY;
        if (!ALLOWED_PRIORITIES.contains(safePriority)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工单优先级不正确");
        }
        return safePriority;
    }

    private String normalizeOptionalPriority(String priority) {
        return StringUtils.hasText(priority) ? normalizePriority(priority) : null;
    }

    private String normalizeOptionalStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return parseStatus(status.trim().toUpperCase()).name();
    }

    private String normalizeScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return null;
        }
        String safeScope = scope.trim();
        if (Set.of("created", "assigned", "watching").contains(safeScope)) {
            return safeScope;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "工单查询范围不正确");
    }

    private String normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        LinkedHashSet<String> normalizedTags = new LinkedHashSet<>();
        for (String tag : tags) {
            if (!StringUtils.hasText(tag)) {
                continue;
            }
            String normalizedTag = tag.trim().replace(",", "");
            if (normalizedTag.length() > MAX_TAG_LENGTH) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "单个标签不能超过30个字符");
            }
            normalizedTags.add(normalizedTag);
        }
        if (normalizedTags.size() > MAX_TAG_COUNT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工单标签不能超过20个");
        }
        return normalizedTags.isEmpty() ? null : String.join(",", normalizedTags);
    }

    private LocalDateTime resolveDueTime(String dueTime, TicketCategory category, LocalDateTime baseTime) {
        LocalDateTime parsedDueTime = parseOptionalDateTime(dueTime, "截止时间");
        if (parsedDueTime != null) {
            return parsedDueTime;
        }
        return category.getDefaultSlaHours() == null ? null : baseTime.plusHours(category.getDefaultSlaHours());
    }

    private LocalDateTime parseOptionalDateTime(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        try {
            return text.contains("T") ? LocalDateTime.parse(text) : LocalDateTime.parse(text, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + "格式不正确");
        }
    }

    private Integer resolveOverdue(LocalDateTime dueTime, LocalDateTime now) {
        return dueTime != null && dueTime.isBefore(now) ? 1 : 0;
    }

    private TicketStatus parseStatus(String status) {
        try {
            return TicketStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "工单状态不正确");
        }
    }

    private void ensureReason(String reason, String message) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }

    /**
     * 归一化完成动作的结构化解决方案。
     *
     * <p>旧客户端仅传 completeRemark 时，将其同时作为摘要和步骤，避免破坏已有接口；新版必须分别提供摘要和步骤。</p>
     */
    private CompletionResolution resolveCompletionResolution(TicketCompleteRequest request) {
        String legacyRemark = request == null ? null : trimToNull(request.getCompleteRemark());
        String summary = request == null ? null : trimToNull(request.getResolutionSummary());
        String steps = request == null ? null : trimToNull(request.getResolutionSteps());
        summary = summary == null ? legacyRemark : summary;
        steps = steps == null ? legacyRemark : steps;
        if (!StringUtils.hasText(summary) || !StringUtils.hasText(steps)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写解决方案摘要和处理步骤");
        }
        if (summary.length() > MAX_RESOLUTION_SUMMARY_LENGTH) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "解决方案摘要不能超过1000个字符");
        }
        return new CompletionResolution(summary, steps, request != null && Boolean.TRUE.equals(request.getResolutionVerified()));
    }

    private String contentOrDefault(String content, String defaultContent) {
        return StringUtils.hasText(content) ? content.trim() : defaultContent;
    }

    /** 完成动作的内部归一化结果，避免散落处理新旧请求字段的兼容逻辑。 */
    private record CompletionResolution(String summary, String steps, boolean verified) {
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String operationType(TicketAction action) {
        return "TICKET_" + action.name();
    }

    private record SearchCondition(String scope,
                                   String ticketNo,
                                   String keyword,
                                   String status,
                                   String priority,
                                   Long categoryId,
                                   Long creatorId,
                                   Long assigneeId,
                                   Long teamId,
                                   Integer overdue,
                                   LocalDateTime createdFrom,
                                   LocalDateTime createdTo) {
    }
}
