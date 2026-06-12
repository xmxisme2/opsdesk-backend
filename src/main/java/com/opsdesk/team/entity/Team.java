package com.opsdesk.team.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 工单处理团队实体。
 *
 * <p>映射 team 表，维护处理团队基础信息，为工单分派和团队看板提供组织基础。</p>
 */
@Getter
@Setter
public class Team {

    private Long id;
    private String name;
    private String description;
    private String processingScope;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    private Integer deleted;
}
