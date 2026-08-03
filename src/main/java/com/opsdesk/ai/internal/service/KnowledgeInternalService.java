package com.opsdesk.ai.internal.service;

import com.opsdesk.ai.internal.dto.ArticleAccessCheckRequest;
import com.opsdesk.ai.internal.dto.IndexSnapshotRequest;
import com.opsdesk.ai.internal.dto.PublishedSnapshotPageRequest;
import com.opsdesk.ai.internal.vo.ArticleAccessCheckVO;
import com.opsdesk.ai.internal.vo.KnowledgeIndexSnapshotVO;
import com.opsdesk.ai.internal.vo.PublishedSnapshotPageVO;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.util.IdParser;
import com.opsdesk.integration.knowledge.KnowledgeOutboxEventService;
import com.opsdesk.knowledge.entity.KnowledgeTag;
import com.opsdesk.knowledge.mapper.KnowledgeArticleMapper;
import com.opsdesk.knowledge.mapper.KnowledgeArticleRow;
import com.opsdesk.knowledge.mapper.KnowledgeTagMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 主应用提供给 AI 服务的受控知识读取服务。
 *
 * <p>AI 服务只能读取文章当前版本，事件版本落后时返回新版本，事件版本超前时拒绝建立错误索引。</p>
 */
@Service
public class KnowledgeInternalService {
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeTagMapper tagMapper;
    private final KnowledgeOutboxEventService eventService;

    public KnowledgeInternalService(KnowledgeArticleMapper articleMapper,
                                    KnowledgeTagMapper tagMapper,
                                    KnowledgeOutboxEventService eventService) {
        this.articleMapper = articleMapper;
        this.tagMapper = tagMapper;
        this.eventService = eventService;
    }

    public KnowledgeIndexSnapshotVO snapshot(String id, IndexSnapshotRequest request) {
        Long articleId = IdParser.parseRequired(id, "知识文章ID");
        KnowledgeArticleRow row = articleMapper.findById(articleId);
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "知识文章不存在");
        }
        long version = row.getVersion() == null ? 1L : row.getVersion();
        if (version < request.expectedVersion()) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "文章版本尚未达到事件期望版本");
        }
        return toSnapshot(row);
    }

    public PublishedSnapshotPageVO publishedSnapshots(PublishedSnapshotPageRequest request) {
        Long afterId = request.afterId() == null || request.afterId().isBlank()
                || "0".equals(request.afterId().trim())
                ? 0L : IdParser.parseRequired(request.afterId(), "文章游标");
        int limit = request.limit() == null ? 100 : Math.min(request.limit(), 200);
        List<KnowledgeArticleRow> rows = articleMapper.findPublishedAfterId(afterId, limit + 1);
        boolean hasMore = rows.size() > limit;
        List<KnowledgeArticleRow> pageRows = hasMore ? rows.subList(0, limit) : rows;
        List<KnowledgeIndexSnapshotVO> items = pageRows.stream().map(this::toSnapshot).toList();
        String nextAfterId = pageRows.isEmpty() ? String.valueOf(afterId)
                : String.valueOf(pageRows.get(pageRows.size() - 1).getId());
        return new PublishedSnapshotPageVO(items, nextAfterId, hasMore);
    }

    private KnowledgeIndexSnapshotVO toSnapshot(KnowledgeArticleRow row) {
        List<KnowledgeIndexSnapshotVO.TagVO> tags = articleMapper.findTagIds(row.getId()).stream()
                .map(tagMapper::findById)
                .filter(java.util.Objects::nonNull)
                .map(this::toTag)
                .toList();
        return new KnowledgeIndexSnapshotVO(
                String.valueOf(row.getId()),
                row.getVersion() == null ? 1L : row.getVersion(),
                row.getTitle(),
                row.getSummary(),
                STATUS_PUBLISHED.equals(row.getStatus()) ? row.getContent() : null,
                row.getCategoryId() == null ? null : String.valueOf(row.getCategoryId()),
                row.getCategoryName(),
                tags,
                row.getSourceTicketId() == null ? null : String.valueOf(row.getSourceTicketId()),
                row.getStatus(),
                "ALL_AUTHENTICATED",
                List.of(),
                List.of(),
                row.getPublishedTime(),
                row.getUpdateTime(),
                eventService.contentHash(row)
        );
    }

    public ArticleAccessCheckVO accessCheck(ArticleAccessCheckRequest request) {
        List<String> accessible = new ArrayList<>();
        List<String> denied = new ArrayList<>();
        String requiredStatus = request.requiredStatus() == null ? STATUS_PUBLISHED : request.requiredStatus();
        for (String id : request.articleIds()) {
            try {
                KnowledgeArticleRow row = articleMapper.findById(IdParser.parseRequired(id, "知识文章ID"));
                if (row != null && requiredStatus.equals(row.getStatus())) {
                    accessible.add(id);
                } else {
                    denied.add(id);
                }
            } catch (BusinessException exception) {
                denied.add(id);
            }
        }
        return new ArticleAccessCheckVO(accessible, denied);
    }

    private KnowledgeIndexSnapshotVO.TagVO toTag(KnowledgeTag tag) {
        return new KnowledgeIndexSnapshotVO.TagVO(String.valueOf(tag.getId()), tag.getName());
    }
}
