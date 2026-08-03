package com.opsdesk.ai.internal.vo;

import java.time.LocalDateTime;
import java.util.List;

/** 知识文章当前索引快照，正文只允许在受保护的服务间接口返回。 */
public record KnowledgeIndexSnapshotVO(
        String articleId,
        long version,
        String title,
        String summary,
        String content,
        String categoryId,
        String categoryName,
        List<TagVO> tags,
        String sourceTicketId,
        String status,
        String visibility,
        List<String> allowedRoleCodes,
        List<String> allowedDepartmentIds,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt,
        String contentHash
) {
    /** 索引所需的标签最小字段。 */
    public record TagVO(String id, String name) {
    }
}
