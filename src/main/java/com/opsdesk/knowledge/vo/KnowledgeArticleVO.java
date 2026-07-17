package com.opsdesk.knowledge.vo;

import com.opsdesk.attachment.vo.AttachmentVO;
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
    /** 已绑定附件，具体预览和下载仍由附件接口进行资源范围校验。 */
    private List<AttachmentVO> attachments;
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
