package com.opsdesk.knowledge.controller;

import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.knowledge.dto.*;
import com.opsdesk.knowledge.service.KnowledgeService;
import com.opsdesk.knowledge.vo.KnowledgeArticleVO;
import com.opsdesk.knowledge.vo.KnowledgeCategoryVO;
import com.opsdesk.knowledge.vo.KnowledgeTagVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 知识库 Controller，只负责鉴权入口、参数接收、限流幂等和服务调用。 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final KnowledgeService service;
    public KnowledgeController(KnowledgeService service) { this.service = service; }
    private String ip(HttpServletRequest r){return r.getRemoteAddr();}
    private String ua(HttpServletRequest r){return r.getHeader("User-Agent");}

    @PostMapping({"/articles/search","/search"}) @PreAuthorize("isAuthenticated()")
    @RateLimit(limit=RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,windowSeconds=RateLimitDefaults.ONE_MINUTE_SECONDS,keyType=RateLimitKeyType.USER)
    public ApiResponse<PageResult<KnowledgeArticleVO>> search(@RequestBody(required=false) KnowledgeArticleSearchRequest request,@AuthenticationPrincipal CurrentUser user){return ApiResponse.success(service.search(request,user));}

    @PostMapping("/articles/{id}/detail") @PreAuthorize("isAuthenticated()")
    public ApiResponse<KnowledgeArticleVO> detail(@PathVariable String id,@AuthenticationPrincipal CurrentUser user){return ApiResponse.success(service.detail(id,user));}

    @PostMapping("/articles/create") @PreAuthorize("hasAnyRole('AGENT','MANAGER','ADMIN')") @Idempotent
    public ApiResponse<KnowledgeArticleVO> create(@Valid @RequestBody KnowledgeArticleMutationRequest request,@AuthenticationPrincipal CurrentUser user,HttpServletRequest servlet){return ApiResponse.success(service.create(request,user,ip(servlet),ua(servlet)));}

    @PostMapping("/articles/{id}/update") @PreAuthorize("hasAnyRole('AGENT','MANAGER','ADMIN')") @Idempotent
    public ApiResponse<KnowledgeArticleVO> update(@PathVariable String id,@Valid @RequestBody KnowledgeArticleMutationRequest request,@AuthenticationPrincipal CurrentUser user,HttpServletRequest servlet){return ApiResponse.success(service.update(id,request,user,ip(servlet),ua(servlet)));}

    @PostMapping("/articles/{id}/delete") @PreAuthorize("hasAnyRole('MANAGER','ADMIN')") @Idempotent
    public ApiResponse<Void> delete(@PathVariable String id,@Valid @RequestBody(required=false) KnowledgeActionRequest request,@AuthenticationPrincipal CurrentUser user,HttpServletRequest servlet){service.delete(id,request,user,ip(servlet),ua(servlet));return ApiResponse.success();}

    @PostMapping("/articles/from-ticket/{ticketId}") @PreAuthorize("hasAnyRole('AGENT','MANAGER','ADMIN')") @Idempotent
    public ApiResponse<KnowledgeArticleVO> fromTicket(@PathVariable String ticketId,@RequestBody(required=false) KnowledgeFromTicketRequest request,@AuthenticationPrincipal CurrentUser user,HttpServletRequest servlet){return ApiResponse.success(service.fromTicket(ticketId,request,user,ip(servlet),ua(servlet)));}

    @PostMapping("/articles/{id}/publish") @PreAuthorize("hasAnyRole('MANAGER','ADMIN')") @Idempotent
    public ApiResponse<KnowledgeArticleVO> publish(@PathVariable String id,@Valid @RequestBody(required=false) KnowledgeActionRequest request,@AuthenticationPrincipal CurrentUser user,HttpServletRequest servlet){return ApiResponse.success(service.publish(id,request,user,ip(servlet),ua(servlet)));}

    @PostMapping("/articles/{id}/offline") @PreAuthorize("hasAnyRole('MANAGER','ADMIN')") @Idempotent
    public ApiResponse<KnowledgeArticleVO> offline(@PathVariable String id,@Valid @RequestBody(required=false) KnowledgeActionRequest request,@AuthenticationPrincipal CurrentUser user,HttpServletRequest servlet){return ApiResponse.success(service.offline(id,request,user,ip(servlet),ua(servlet)));}

    @PostMapping("/categories/tree") @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<KnowledgeCategoryVO>> categories(@RequestBody(required=false) KnowledgeCategoryTreeRequest request){return ApiResponse.success(service.categoryTree(request));}

    @PostMapping("/categories/create") @PreAuthorize("hasAnyRole('MANAGER','ADMIN')") @Idempotent
    public ApiResponse<KnowledgeCategoryVO> createCategory(@Valid @RequestBody KnowledgeCategoryMutationRequest request,@AuthenticationPrincipal CurrentUser user,HttpServletRequest servlet){return ApiResponse.success(service.createCategory(request,user,ip(servlet),ua(servlet)));}

    @PostMapping("/categories/{id}/update") @PreAuthorize("hasAnyRole('MANAGER','ADMIN')") @Idempotent
    public ApiResponse<KnowledgeCategoryVO> updateCategory(@PathVariable String id,@Valid @RequestBody KnowledgeCategoryMutationRequest request,@AuthenticationPrincipal CurrentUser user,HttpServletRequest servlet){return ApiResponse.success(service.updateCategory(id,request,user,ip(servlet),ua(servlet)));}

    @PostMapping("/categories/{id}/delete") @PreAuthorize("hasAnyRole('MANAGER','ADMIN')") @Idempotent
    public ApiResponse<Void> deleteCategory(@PathVariable String id,@AuthenticationPrincipal CurrentUser user,HttpServletRequest servlet){service.deleteCategory(id,user,ip(servlet),ua(servlet));return ApiResponse.success();}

    @PostMapping("/tags/search") @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<KnowledgeTagVO>> tags(@RequestBody(required=false) KnowledgeTagSearchRequest request){return ApiResponse.success(service.searchTags(request));}

    @PostMapping("/tags/create") @PreAuthorize("hasAnyRole('AGENT','MANAGER','ADMIN')") @Idempotent
    public ApiResponse<KnowledgeTagVO> createTag(@Valid @RequestBody KnowledgeTagCreateRequest request,@AuthenticationPrincipal CurrentUser user){return ApiResponse.success(service.createTag(request,user));}

    @PostMapping("/tags/{id}/delete") @PreAuthorize("hasAnyRole('MANAGER','ADMIN')") @Idempotent
    public ApiResponse<Void> deleteTag(@PathVariable String id,@AuthenticationPrincipal CurrentUser user){service.deleteTag(id,user);return ApiResponse.success();}
}
