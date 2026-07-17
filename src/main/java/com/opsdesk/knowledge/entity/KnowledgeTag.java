package com.opsdesk.knowledge.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 知识库标签实体，articleCount 用于列表筛选和删除保护。
 */
@Getter
@Setter
public class KnowledgeTag {
    private Long id;
    private String name;
    private Integer articleCount;
    private Long createBy;
    private Long updateBy;
}
