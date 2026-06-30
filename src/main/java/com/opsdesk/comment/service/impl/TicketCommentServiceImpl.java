package com.opsdesk.comment.service.impl;

import com.opsdesk.attachment.converter.AttachmentConverter;
import com.opsdesk.attachment.entity.Attachment;
import com.opsdesk.attachment.mapper.AttachmentMapper;
import com.opsdesk.attachment.service.AttachmentResourceAccessService;
import com.opsdesk.attachment.vo.AttachmentVO;
import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.comment.converter.TicketCommentConverter;
import com.opsdesk.comment.dto.CommentCreateRequest;
import com.opsdesk.comment.dto.CommentSearchRequest;
import com.opsdesk.comment.entity.TicketComment;
import com.opsdesk.comment.mapper.TicketCommentMapper;
import com.opsdesk.comment.service.TicketCommentService;
import com.opsdesk.comment.vo.CommentVO;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.common.util.IdParser;
import com.opsdesk.team.mapper.TeamMemberMapper;
import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.ticket.entity.TicketOperationLog;
import com.opsdesk.ticket.mapper.TicketMapper;
import com.opsdesk.ticket.mapper.TicketOperationLogMapper;
import com.opsdesk.user.entity.SysUser;
import com.opsdesk.user.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 工单评论服务实现。
 *
 * <p>集中处理评论资源范围、内部备注权限、临时附件绑定和删除审计，Controller 不直接访问评论表。</p>
 */
@Service
public class TicketCommentServiceImpl implements TicketCommentService {

    /** 公开评论类型：工单参与人均可见，允许外部按契约传入。 */
    private static final String COMMENT_TYPE_PUBLIC = "PUBLIC";

    /** 内部备注类型：仅处理人、团队成员、MANAGER、ADMIN 和作者可见，允许外部按契约传入。 */
    private static final String COMMENT_TYPE_INTERNAL = "INTERNAL";

    /** 评论内容最大长度：防止长文本挤占详情页和数据库资源，外部超过会返回参数错误。 */
    private static final int MAX_CONTENT_LENGTH = 5000;

    /** 临时附件令牌格式：与附件上传保持一致，只允许安全字符，不允许路径字符。 */
    private static final Pattern TEMP_TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{16,64}");

    /** 管理员角色编码：可访问全局评论、删除任意评论，由认证上下文提供。 */
    private static final String ROLE_ADMIN = "ADMIN";

    /** 团队负责人角色编码：可查看内部备注，但仍需满足工单参与范围。 */
    private static final String ROLE_MANAGER = "MANAGER";

    /** 工单处理角色编码：可发表和查看内部备注，但仍需满足工单参与范围。 */
    private static final String ROLE_AGENT = "AGENT";

    /** 评论审计业务类型：audit_log.biz_type 固定写 COMMENT。 */
    private static final String AUDIT_BIZ_TYPE = "COMMENT";

    /** 删除审计动作：评论逻辑删除成功后记录。 */
    private static final String AUDIT_OPERATION_DELETE = "COMMENT_DELETE";

    /** 工单评论新增日志类型：写入 ticket_operation_log，用于工单详情时间线。 */
    private static final String OPERATION_COMMENT_CREATE = "COMMENT_CREATE";

    /** 工单评论删除日志类型：写入 ticket_operation_log，用于追踪评论删除动作。 */
    private static final String OPERATION_COMMENT_DELETE = "COMMENT_DELETE";

    private final TicketCommentMapper commentMapper;
    private final TicketMapper ticketMapper;
    private final TicketOperationLogMapper ticketOperationLogMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final SysUserMapper sysUserMapper;
    private final AttachmentMapper attachmentMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final AuditLogService auditLogService;
    private final TicketCommentConverter commentConverter;
    private final AttachmentConverter attachmentConverter;

    @Autowired
    public TicketCommentServiceImpl(TicketCommentMapper commentMapper,
                                    TicketMapper ticketMapper,
                                    TicketOperationLogMapper ticketOperationLogMapper,
                                    TeamMemberMapper teamMemberMapper,
                                    SysUserMapper sysUserMapper,
                                    AttachmentMapper attachmentMapper,
                                    SnowflakeIdGenerator idGenerator,
                                    AuditLogService auditLogService) {
        this(commentMapper, ticketMapper, ticketOperationLogMapper, teamMemberMapper, sysUserMapper, attachmentMapper, idGenerator,
                auditLogService, new TicketCommentConverter(), new AttachmentConverter());
    }

    public TicketCommentServiceImpl(TicketCommentMapper commentMapper,
                                    TicketMapper ticketMapper,
                                    TicketOperationLogMapper ticketOperationLogMapper,
                                    TeamMemberMapper teamMemberMapper,
                                    SysUserMapper sysUserMapper,
                                    AttachmentMapper attachmentMapper,
                                    SnowflakeIdGenerator idGenerator,
                                    AuditLogService auditLogService,
                                    TicketCommentConverter commentConverter,
                                    AttachmentConverter attachmentConverter) {
        this.commentMapper = commentMapper;
        this.ticketMapper = ticketMapper;
        this.ticketOperationLogMapper = ticketOperationLogMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.sysUserMapper = sysUserMapper;
        this.attachmentMapper = attachmentMapper;
        this.idGenerator = idGenerator;
        this.auditLogService = auditLogService;
        this.commentConverter = commentConverter;
        this.attachmentConverter = attachmentConverter;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentVO create(String ticketId,
                            CommentCreateRequest request,
                            CurrentUser currentUser,
                            String requestIp,
                            String userAgent) {
        Long operatorId = requireUserId(currentUser);
        Ticket ticket = requireAccessibleTicket(parseRequiredId(ticketId, "工单ID"), currentUser);
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评论请求不能为空");
        }
        String commentType = normalizeCommentType(request.getCommentType());
        if (COMMENT_TYPE_INTERNAL.equals(commentType) && !canUseInternalComment(ticket, currentUser)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无权发表内部备注");
        }

        TicketComment comment = new TicketComment();
        comment.setId(idGenerator.nextId());
        comment.setTicketId(ticket.getId());
        comment.setContent(normalizeContent(request.getContent()));
        comment.setCommentType(commentType);
        comment.setAuthorId(operatorId);
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(comment.getCreateTime());
        comment.setCreateBy(operatorId);
        comment.setUpdateBy(operatorId);
        comment.setDeleted(0);
        if (commentMapper.insert(comment) == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "评论保存失败");
        }
        bindTempAttachments(request.getTempToken(), operatorId, comment.getId());
        recordTicketLog(ticket, OPERATION_COMMENT_CREATE, operatorId, "新增工单评论", requestIp, userAgent);
        return assembleCommentVO(comment);
    }

    @Override
    public PageResult<CommentVO> search(String ticketId, CommentSearchRequest request, CurrentUser currentUser) {
        Ticket ticket = requireAccessibleTicket(parseRequiredId(ticketId, "工单ID"), currentUser);
        CommentSearchRequest safeRequest = request == null ? new CommentSearchRequest() : request;
        long page = safeRequest.normalizedPage();
        long size = safeRequest.normalizedSize();
        boolean includeInternal = canViewInternalComment(ticket, currentUser);
        long total = commentMapper.countByTicketId(ticket.getId(), includeInternal);
        if (total == 0) {
            return PageResult.empty(page, size);
        }
        long offset = (page - 1) * size;
        List<CommentVO> records = commentMapper.searchByTicketId(ticket.getId(), includeInternal, offset, size)
                .stream()
                .map(this::assembleCommentVO)
                .toList();
        return new PageResult<>(records, page, size, total);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id, CurrentUser currentUser, String requestIp, String userAgent) {
        Long operatorId = requireUserId(currentUser);
        TicketComment comment = requireComment(parseRequiredId(id, "评论ID"));
        Ticket ticket = requireAccessibleTicket(comment.getTicketId(), currentUser);
        if (!sameUser(operatorId, comment.getAuthorId()) && !hasRole(currentUser, ROLE_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅评论作者或管理员可删除评论");
        }
        if (COMMENT_TYPE_INTERNAL.equals(comment.getCommentType()) && !canViewInternalComment(ticket, currentUser)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无权删除内部备注");
        }
        if (commentMapper.logicalDelete(comment.getId(), operatorId) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }
        recordTicketLog(ticket, OPERATION_COMMENT_DELETE, operatorId, "删除工单评论", requestIp, userAgent);
        auditLogService.record(operatorId, AUDIT_OPERATION_DELETE, AUDIT_BIZ_TYPE, comment.getId(),
                "删除工单评论", requestIp, userAgent);
    }

    private void recordTicketLog(Ticket ticket,
                                 String operationType,
                                 Long operatorId,
                                 String content,
                                 String requestIp,
                                 String userAgent) {
        TicketOperationLog log = new TicketOperationLog();
        log.setId(idGenerator.nextId());
        log.setTicketId(ticket.getId());
        log.setOperationType(operationType);
        log.setFromStatus(ticket.getStatus());
        log.setToStatus(ticket.getStatus());
        log.setOperatorId(operatorId);
        log.setContent(content);
        log.setRequestIp(requestIp);
        log.setUserAgent(userAgent);
        log.setCreateBy(operatorId);
        log.setUpdateBy(operatorId);
        ticketOperationLogMapper.insert(log);
    }

    private void bindTempAttachments(String tempToken, Long operatorId, Long commentId) {
        if (!StringUtils.hasText(tempToken)) {
            return;
        }
        String normalized = tempToken.trim();
        if (!TEMP_TOKEN_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "tempToken格式不正确");
        }
        attachmentMapper.bindTempToBiz(AttachmentResourceAccessService.BIZ_TYPE_COMMENT, normalized, operatorId, commentId, operatorId);
    }

    private CommentVO assembleCommentVO(TicketComment comment) {
        return commentConverter.toVO(comment, findUser(comment.getAuthorId()), findCommentAttachments(comment.getId()));
    }

    private List<AttachmentVO> findCommentAttachments(Long commentId) {
        List<Attachment> attachments = attachmentMapper.findByBiz(AttachmentResourceAccessService.BIZ_TYPE_COMMENT, commentId);
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments.stream().map(attachmentConverter::toVO).toList();
    }

    private Ticket requireAccessibleTicket(Long ticketId, CurrentUser currentUser) {
        Ticket ticket = ticketMapper.findById(ticketId);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        }
        Long userId = requireUserId(currentUser);
        if (hasRole(currentUser, ROLE_ADMIN)
                || sameUser(userId, ticket.getCreatorId())
                || sameUser(userId, ticket.getAssigneeId())
                || isTeamMember(ticket.getTeamId(), userId)) {
            return ticket;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无权访问该工单");
    }

    private TicketComment requireComment(Long commentId) {
        TicketComment comment = commentMapper.findById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }
        return comment;
    }

    private boolean canUseInternalComment(Ticket ticket, CurrentUser currentUser) {
        Long userId = requireUserId(currentUser);
        return hasAnyRole(currentUser, ROLE_AGENT, ROLE_MANAGER, ROLE_ADMIN)
                || sameUser(userId, ticket.getAssigneeId())
                || isTeamMember(ticket.getTeamId(), userId);
    }

    private boolean canViewInternalComment(Ticket ticket, CurrentUser currentUser) {
        return canUseInternalComment(ticket, currentUser);
    }

    private String normalizeCommentType(String commentType) {
        String normalized = StringUtils.hasText(commentType) ? commentType.trim().toUpperCase(Locale.ROOT) : COMMENT_TYPE_PUBLIC;
        if (!Set.of(COMMENT_TYPE_PUBLIC, COMMENT_TYPE_INTERNAL).contains(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评论类型不正确");
        }
        return normalized;
    }

    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评论内容不能为空");
        }
        String normalized = content.trim();
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评论内容不能超过5000个字符");
        }
        return normalized;
    }

    private SysUser findUser(Long userId) {
        return userId == null ? null : sysUserMapper.findById(userId);
    }

    private boolean isTeamMember(Long teamId, Long userId) {
        return teamId != null && userId != null && teamMemberMapper.countActive(teamId, userId) > 0;
    }

    private boolean hasRole(CurrentUser currentUser, String role) {
        return currentUser != null && currentUser.getRoles().contains(role);
    }

    private boolean hasAnyRole(CurrentUser currentUser, String... roles) {
        for (String role : roles) {
            if (hasRole(currentUser, role)) {
                return true;
            }
        }
        return false;
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
}
