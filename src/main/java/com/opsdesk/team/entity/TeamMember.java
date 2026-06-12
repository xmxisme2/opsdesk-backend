package com.opsdesk.team.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 团队成员实体。
 *
 * <p>映射 team_member 表，记录用户加入团队、是否负责人和团队内角色。</p>
 */
@Getter
@Setter
public class TeamMember {

    private Long id;
    private Long teamId;
    private Long userId;
    private String memberRole;
    private Integer leaderFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    private Integer deleted;
}
