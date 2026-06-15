package com.opsdesk.ticket.service;

import java.util.Set;

/**
 * 工单状态动作的操作者上下文。
 *
 * <p>Service 在调用状态机前填充创建人、当前处理人、角色和团队处理权限，状态机只依赖这些领域信息判断是否允许流转。</p>
 */
public class TicketStateContext {

    /** 管理员角色编码：可做全局管理动作，由后端角色体系写入，外部不可任意传入生效。 */
    private static final String ROLE_ADMIN = "ADMIN";

    /** 团队负责人角色编码：可管理本团队工单，由后端资源范围校验后写入，外部不可任意传入生效。 */
    private static final String ROLE_MANAGER = "MANAGER";

    /** 处理人角色编码：可处理分配给自己或本团队的工单，由后端认证上下文提供。 */
    private static final String ROLE_AGENT = "AGENT";

    private final Long operatorId;
    private final Long creatorId;
    private final Long assigneeId;
    private final Set<String> roles;
    private final boolean teamMember;

    private TicketStateContext(Long operatorId, Long creatorId, Long assigneeId, Set<String> roles, boolean teamMember) {
        this.operatorId = operatorId;
        this.creatorId = creatorId;
        this.assigneeId = assigneeId;
        this.roles = roles == null ? Set.of() : Set.copyOf(roles);
        this.teamMember = teamMember;
    }

    public static TicketStateContext of(Long operatorId, Long creatorId, Long assigneeId, Set<String> roles, boolean teamMember) {
        return new TicketStateContext(operatorId, creatorId, assigneeId, roles, teamMember);
    }

    public boolean isCreator() {
        return sameUser(operatorId, creatorId);
    }

    public boolean isCurrentAssignee() {
        return sameUser(operatorId, assigneeId);
    }

    public boolean isTeamMember() {
        return teamMember;
    }

    public boolean isAdmin() {
        return roles.contains(ROLE_ADMIN);
    }

    public boolean isManagerOrAdmin() {
        return roles.contains(ROLE_MANAGER) || isAdmin();
    }

    public boolean isAgentOrAbove() {
        return roles.contains(ROLE_AGENT) || isManagerOrAdmin();
    }

    private boolean sameUser(Long left, Long right) {
        return left != null && left.equals(right);
    }
}
