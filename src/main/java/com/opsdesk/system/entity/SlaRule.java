package com.opsdesk.system.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** SLA 规则实体，定义分类与优先级组合对应的响应和解决时限。 */
@Getter
@Setter
public class SlaRule {
    private Long id;
    private Long categoryId;
    private String priority;
    private Integer responseHours;
    private Integer resolveHours;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    private Integer deleted;
}
