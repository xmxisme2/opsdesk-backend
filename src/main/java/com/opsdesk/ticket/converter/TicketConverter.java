package com.opsdesk.ticket.converter;

import com.opsdesk.attachment.vo.AttachmentVO;
import com.opsdesk.team.entity.Team;
import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.ticket.entity.TicketCategory;
import com.opsdesk.ticket.entity.TicketOperationLog;
import com.opsdesk.ticket.vo.TicketCategoryVO;
import com.opsdesk.ticket.vo.TicketListItemVO;
import com.opsdesk.ticket.vo.TicketOperationLogVO;
import com.opsdesk.ticket.vo.TicketVO;
import com.opsdesk.user.entity.SysUser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 工单对象转换器。
 *
 * <p>集中处理 Entity 到 VO 的字段转换、ID 字符串化、时间格式化和展示名称解析。</p>
 */
@Component
public class TicketConverter {

    /** 时间输出格式：与后台其他列表和详情接口保持一致，便于前端统一渲染。 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TicketVO toVO(Ticket ticket,
                         TicketCategory category,
                         SysUser creator,
                         SysUser assignee,
                         Team team,
                         boolean watching) {
        return toVO(ticket, category, creator, assignee, team, watching, List.of(), List.of());
    }

    public TicketVO toVO(Ticket ticket,
                         TicketCategory category,
                         SysUser creator,
                         SysUser assignee,
                         Team team,
                         boolean watching,
                         List<AttachmentVO> attachments) {
        return toVO(ticket, category, creator, assignee, team, watching, attachments, List.of());
    }

    public TicketVO toVO(Ticket ticket,
                         TicketCategory category,
                         SysUser creator,
                         SysUser assignee,
                         Team team,
                         boolean watching,
                         List<AttachmentVO> attachments,
                         List<String> availableActions) {
        return new TicketVO(
                String.valueOf(ticket.getId()),
                ticket.getTicketNo(),
                ticket.getTitle(),
                ticket.getDescription(),
                String.valueOf(ticket.getCategoryId()),
                category == null ? null : category.getName(),
                ticket.getPriority(),
                ticket.getStatus(),
                String.valueOf(ticket.getCreatorId()),
                resolveUserName(creator),
                ticket.getAssigneeId() == null ? null : String.valueOf(ticket.getAssigneeId()),
                resolveUserName(assignee),
                ticket.getTeamId() == null ? null : String.valueOf(ticket.getTeamId()),
                team == null ? null : team.getName(),
                format(ticket.getDueTime()),
                format(ticket.getCompletedTime()),
                format(ticket.getClosedTime()),
                ticket.getOverdue() != null && ticket.getOverdue() == 1,
                splitTags(ticket.getTags()),
                watching,
                attachments == null ? List.of() : attachments,
                availableActions == null ? List.of() : availableActions,
                format(ticket.getCreateTime()),
                format(ticket.getUpdateTime())
        );
    }

    public TicketListItemVO toListItem(Ticket ticket,
                                       TicketCategory category,
                                       SysUser creator,
                                       SysUser assignee,
                                       Team team) {
        return new TicketListItemVO(
                String.valueOf(ticket.getId()),
                ticket.getTicketNo(),
                ticket.getTitle(),
                category == null ? null : category.getName(),
                ticket.getPriority(),
                ticket.getStatus(),
                resolveUserName(creator),
                resolveUserName(assignee),
                team == null ? null : team.getName(),
                format(ticket.getDueTime()),
                ticket.getOverdue() != null && ticket.getOverdue() == 1,
                format(ticket.getCreateTime()),
                format(ticket.getUpdateTime())
        );
    }

    public TicketCategoryVO toCategoryVO(TicketCategory category,
                                         Team defaultTeam,
                                         List<TicketCategoryVO> children) {
        return new TicketCategoryVO(
                String.valueOf(category.getId()),
                category.getParentId() == null ? null : String.valueOf(category.getParentId()),
                category.getName(),
                category.getDefaultTeamId() == null ? null : String.valueOf(category.getDefaultTeamId()),
                defaultTeam == null ? null : defaultTeam.getName(),
                category.getDefaultSlaHours(),
                category.getSort(),
                category.getEnabled() != null && category.getEnabled() == 1,
                format(category.getCreateTime()),
                format(category.getUpdateTime()),
                children
        );
    }

    public TicketOperationLogVO toOperationLogVO(TicketOperationLog log, SysUser operator) {
        return new TicketOperationLogVO(
                String.valueOf(log.getId()),
                String.valueOf(log.getTicketId()),
                log.getOperationType(),
                log.getFromStatus(),
                log.getToStatus(),
                String.valueOf(log.getOperatorId()),
                resolveUserName(operator),
                log.getContent(),
                log.getRequestIp(),
                log.getUserAgent(),
                format(log.getCreateTime())
        );
    }

    private List<String> splitTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String resolveUserName(SysUser user) {
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private String format(LocalDateTime time) {
        return time == null ? null : DATE_TIME_FORMATTER.format(time);
    }
}
