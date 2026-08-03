package com.opsdesk.ai.internal.vo;

import java.util.List;

/** 已发布文章索引快照游标页。 */
public record PublishedSnapshotPageVO(
        List<KnowledgeIndexSnapshotVO> items,
        String nextAfterId,
        boolean hasMore
) {
}
