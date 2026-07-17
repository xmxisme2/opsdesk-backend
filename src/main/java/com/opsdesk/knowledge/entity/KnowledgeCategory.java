package com.opsdesk.knowledge.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 知识库分类实体，支持多级分类树和启停控制。
 */
@Getter
@Setter
public class KnowledgeCategory {
    private Long id;
    private Long parentId;
    private String name;
    private Integer sort;
    private Integer enabled;
    private Long createBy;
    private Long updateBy;
}
