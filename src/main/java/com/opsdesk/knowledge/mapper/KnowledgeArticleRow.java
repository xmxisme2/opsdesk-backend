package com.opsdesk.knowledge.mapper;

import com.opsdesk.knowledge.entity.KnowledgeArticle;
import lombok.Getter;
import lombok.Setter;

/**
 * 文章查询聚合行，补充分类、作者、来源工单和标签展示字段。
 */
@Getter
@Setter
public class KnowledgeArticleRow extends KnowledgeArticle {
    private String categoryName;
    private String authorName;
    private String sourceTicketNo;
    private String tagNames;
}
