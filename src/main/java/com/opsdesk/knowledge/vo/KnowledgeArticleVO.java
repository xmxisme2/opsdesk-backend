package com.opsdesk.knowledge.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/** 知识库文章接口视图，所有 ID 按字符串返回。 */
@Getter
@Setter
public class KnowledgeArticleVO {
    private String id;
    private String title;
    private String summary;
    private String content;
    private String categoryId;
    private String categoryName;
    private List<String> tags;
    private String sourceTicketId;
    private String sourceTicketNo;
    private String status;
    private String authorId;
    private String authorName;
    private long viewCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
