package com.opsdesk.ai.controller;

import com.opsdesk.ai.service.AiHealthProxyService;
import com.opsdesk.ai.service.AiIndexAdminService;
import com.opsdesk.ai.dto.IndexRebuildRequest;
import com.opsdesk.ai.dto.AiConnectionTestRequest;
import com.opsdesk.ai.dto.IndexReindexRequest;
import com.opsdesk.ai.dto.IndexReconcileRequest;
import com.opsdesk.ai.vo.AiServiceHealthVO;
import com.opsdesk.ai.vo.AiConnectionTestVO;
import com.opsdesk.ai.vo.IndexTaskAcceptedVO;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 管理代理 Controller。
 *
 * <p>前端只访问主应用；所有独立 AI 服务调用都在主应用完成鉴权和服务身份签发。</p>
 */
@RestController
@RequestMapping("/api/ai/admin")
public class AiAdminController {

    private final AiHealthProxyService healthProxyService;
    private final AiIndexAdminService indexAdminService;

    public AiAdminController(AiHealthProxyService healthProxyService) {
        this(healthProxyService, null);
    }

    @Autowired
    public AiAdminController(AiHealthProxyService healthProxyService, AiIndexAdminService indexAdminService) {
        this.healthProxyService = healthProxyService;
        this.indexAdminService = indexAdminService;
    }

    @PostMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AiServiceHealthVO> health(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(healthProxyService.check(currentUser));
    }

    @PostMapping("/model/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AiConnectionTestVO> testConnection(
            @Valid @RequestBody AiConnectionTestRequest request,
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(healthProxyService.testConnection(request, currentUser));
    }

    @PostMapping("/index/articles/{id}/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<IndexTaskAcceptedVO> reindex(@PathVariable String id,
                                                    @RequestBody(required = false) IndexReindexRequest request,
                                                    @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(indexAdminService.reindex(id, request, currentUser));
    }

    @PostMapping("/index/rebuild")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<IndexTaskAcceptedVO> rebuild(@Valid @RequestBody IndexRebuildRequest request,
                                                    @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(indexAdminService.rebuild(request, currentUser));
    }

    @PostMapping("/index/reconcile")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<IndexTaskAcceptedVO> reconcile(@Valid @RequestBody IndexReconcileRequest request,
                                                      @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(indexAdminService.reconcile(request, currentUser));
    }
}
