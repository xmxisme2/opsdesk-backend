package com.opsdesk.knowledge.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识库文章实体，映射 knowledge_article 表。
 */
@Getter
@Setter
public class KnowledgeArticle {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private Long categoryId;
    private Long sourceTicketId;
    private String status;
    private Long authorId;
    private Long version;
    private Long viewCount;
    private LocalDateTime publishedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    private Integer deleted;
}
