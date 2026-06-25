package com.opsdesk.attachment.service;

import com.opsdesk.attachment.mapper.AttachmentMapper;
import com.opsdesk.attachment.model.AttachmentResourceInfo;
import com.opsdesk.attachment.model.AttachmentResourceScope;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.team.mapper.TeamMemberMapper;
import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.ticket.mapper.TicketMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * 附件业务资源访问服务。
 *
 * <p>统一校验工单、评论和知识库的资源范围，附件查询、下载、预览、上传和删除不得自行绕过。</p>
 */
@Service
public class AttachmentResourceAccessService {

    /** 工单业务类型：附件直接绑定 ticket.id，允许外部按契约传入。 */
    public static final String BIZ_TYPE_TICKET = "TICKET";

    /** 评论业务类型：附件绑定 ticket_comment.id，并继承所属工单访问范围。 */
    public static final String BIZ_TYPE_COMMENT = "COMMENT";

    /** 知识库业务类型：附件绑定 knowledge_article.id，并按发布状态和维护权限判断。 */
    public static final String BIZ_TYPE_KNOWLEDGE = "KNOWLEDGE";

    /** 管理员角色：可访问全局业务资源，由认证上下文提供，禁止外部请求体伪造。 */
    private static final String ROLE_ADMIN = "ADMIN";

    /** 团队负责人角色：可维护知识库，并通过团队成员关系访问工单。 */
    private static final String ROLE_MANAGER = "MANAGER";

    /** 工单处理人角色：可读取内部评论并维护本人知识库文章。 */
    private static final String ROLE_AGENT = "AGENT";

    /** 内部评论类型：仅处理角色、评论作者和管理员可见。 */
    private static final String COMMENT_TYPE_INTERNAL = "INTERNAL";

    /** 已发布知识库状态：任意登录用户可读取。 */
    private static final String KNOWLEDGE_STATUS_PUBLISHED = "PUBLISHED";

    private final TicketMapper ticketMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final AttachmentMapper attachmentMapper;

    public AttachmentResourceAccessService(TicketMapper ticketMapper,
                                           TeamMemberMapper teamMemberMapper,
                                           AttachmentMapper attachmentMapper) {
        this.ticketMapper = ticketMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.attachmentMapper = attachmentMapper;
    }

    public AttachmentResourceScope requireReadAccess(String bizType, Long bizId, CurrentUser currentUser) {
        String normalizedBizType = normalizeBizType(bizType);
        Long userId = requireUserId(currentUser);
        return switch (normalizedBizType) {
            case BIZ_TYPE_TICKET -> requireTicketAccess(bizId, currentUser);
            case BIZ_TYPE_COMMENT -> requireCommentReadAccess(bizId, currentUser, userId);
            case BIZ_TYPE_KNOWLEDGE -> requireKnowledgeReadAccess(bizId, currentUser, userId);
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的附件业务类型");
        };
    }

    public AttachmentResourceScope requireWriteAccess(String bizType, Long bizId, CurrentUser currentUser) {
        String normalizedBizType = normalizeBizType(bizType);
        Long userId = requireUserId(currentUser);
        return switch (normalizedBizType) {
            case BIZ_TYPE_TICKET -> requireTicketAccess(bizId, currentUser);
            case BIZ_TYPE_COMMENT -> requireCommentWriteAccess(bizId, currentUser, userId);
            case BIZ_TYPE_KNOWLEDGE -> requireKnowledgeWriteAccess(bizId, currentUser, userId);
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的附件业务类型");
        };
    }

    public String normalizeBizType(String bizType) {
        if (!StringUtils.hasText(bizType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "bizType不能为空");
        }
        String normalized = bizType.trim().toUpperCase(Locale.ROOT);
        if (!Set.of(BIZ_TYPE_TICKET, BIZ_TYPE_COMMENT, BIZ_TYPE_KNOWLEDGE).contains(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的附件业务类型");
        }
        return normalized;
    }

    private AttachmentResourceScope requireTicketAccess(Long ticketId, CurrentUser currentUser) {
        Ticket ticket = ticketMapper.findById(requireBizId(ticketId));
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        }
        Long userId = currentUser.getUserId();
        if (hasRole(currentUser, ROLE_ADMIN)
                || sameUser(userId, ticket.getCreatorId())
                || sameUser(userId, ticket.getAssigneeId())
                || isTeamMember(ticket.getTeamId(), userId)) {
            return new AttachmentResourceScope(BIZ_TYPE_TICKET, ticketId, ticketId);
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无权访问该工单");
    }

    private AttachmentResourceScope requireCommentReadAccess(Long commentId,
                                                             CurrentUser currentUser,
                                                             Long userId) {
        AttachmentResourceInfo comment = attachmentMapper.findCommentResource(requireBizId(commentId));
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }
        requireTicketAccess(comment.getTicketId(), currentUser);
        if (COMMENT_TYPE_INTERNAL.equals(comment.getStatus())
                && !sameUser(userId, comment.getOwnerId())
                && !hasAnyRole(currentUser, ROLE_AGENT, ROLE_MANAGER, ROLE_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无权访问内部评论附件");
        }
        return new AttachmentResourceScope(BIZ_TYPE_COMMENT, commentId, comment.getTicketId());
    }

    private AttachmentResourceScope requireCommentWriteAccess(Long commentId,
                                                              CurrentUser currentUser,
                                                              Long userId) {
        AttachmentResourceInfo comment = attachmentMapper.findCommentResource(requireBizId(commentId));
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }
        requireTicketAccess(comment.getTicketId(), currentUser);
        if (!sameUser(userId, comment.getOwnerId()) && !hasRole(currentUser, ROLE_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅评论作者或管理员可维护评论附件");
        }
        return new AttachmentResourceScope(BIZ_TYPE_COMMENT, commentId, comment.getTicketId());
    }

    private AttachmentResourceScope requireKnowledgeReadAccess(Long articleId,
                                                               CurrentUser currentUser,
                                                               Long userId) {
        AttachmentResourceInfo article = attachmentMapper.findKnowledgeResource(requireBizId(articleId));
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库文章不存在");
        }
        if (KNOWLEDGE_STATUS_PUBLISHED.equals(article.getStatus())
                || sameUser(userId, article.getOwnerId())
                || hasAnyRole(currentUser, ROLE_MANAGER, ROLE_ADMIN)) {
            return new AttachmentResourceScope(BIZ_TYPE_KNOWLEDGE, articleId, null);
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无权访问该知识库附件");
    }

    private AttachmentResourceScope requireKnowledgeWriteAccess(Long articleId,
                                                                CurrentUser currentUser,
                                                                Long userId) {
        AttachmentResourceInfo article = attachmentMapper.findKnowledgeResource(requireBizId(articleId));
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识库文章不存在");
        }
        boolean authorCanWrite = sameUser(userId, article.getOwnerId())
                && hasAnyRole(currentUser, ROLE_AGENT, ROLE_MANAGER, ROLE_ADMIN);
        if (authorCanWrite || hasAnyRole(currentUser, ROLE_MANAGER, ROLE_ADMIN)) {
            return new AttachmentResourceScope(BIZ_TYPE_KNOWLEDGE, articleId, null);
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无权维护该知识库附件");
    }

    private Long requireBizId(Long bizId) {
        if (bizId == null || bizId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "bizId必须为正整数");
        }
        return bizId;
    }

    private Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }
        return currentUser.getUserId();
    }

    private boolean isTeamMember(Long teamId, Long userId) {
        return teamId != null && teamMemberMapper.countActive(teamId, userId) > 0;
    }

    private boolean hasRole(CurrentUser currentUser, String role) {
        return currentUser.getRoles().contains(role);
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
}
