package com.opsdesk.ai.internal.controller;

import com.opsdesk.ai.internal.dto.ArticleAccessCheckRequest;
import com.opsdesk.ai.internal.dto.IndexSnapshotRequest;
import com.opsdesk.ai.internal.dto.PublishedSnapshotPageRequest;
import com.opsdesk.ai.internal.service.KnowledgeInternalService;
import com.opsdesk.ai.internal.vo.ArticleAccessCheckVO;
import com.opsdesk.ai.internal.vo.KnowledgeIndexSnapshotVO;
import com.opsdesk.ai.internal.vo.PublishedSnapshotPageVO;
import com.opsdesk.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 服务专用知识内部接口，不对浏览器和普通用户令牌开放。 */
@RestController
@RequestMapping("/internal/knowledge/articles")
public class KnowledgeInternalController {
    private final KnowledgeInternalService service;

    public KnowledgeInternalController(KnowledgeInternalService service) {
        this.service = service;
    }

    @PostMapping("/{id}/index-snapshot")
    public ApiResponse<KnowledgeIndexSnapshotVO> snapshot(@PathVariable String id,
                                                          @Valid @RequestBody IndexSnapshotRequest request) {
        return ApiResponse.success(service.snapshot(id, request));
    }

    @PostMapping("/access-check")
    public ApiResponse<ArticleAccessCheckVO> accessCheck(@Valid @RequestBody ArticleAccessCheckRequest request) {
        return ApiResponse.success(service.accessCheck(request));
    }

    @PostMapping("/published-index-snapshots")
    public ApiResponse<PublishedSnapshotPageVO> publishedSnapshots(
            @Valid @RequestBody PublishedSnapshotPageRequest request) {
        return ApiResponse.success(service.publishedSnapshots(request));
    }
}
