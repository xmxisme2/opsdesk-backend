package com.opsdesk.team.controller;

import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.team.dto.TeamCreateRequest;
import com.opsdesk.team.dto.TeamLeaderUpdateRequest;
import com.opsdesk.team.dto.TeamMemberSearchRequest;
import com.opsdesk.team.dto.TeamMemberUpdateRequest;
import com.opsdesk.team.dto.TeamSearchRequest;
import com.opsdesk.team.dto.TeamUpdateRequest;
import com.opsdesk.team.service.TeamService;
import com.opsdesk.team.vo.TeamMemberVO;
import com.opsdesk.team.vo.TeamVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 团队管理 Controller。
 *
 * <p>提供团队列表、团队基础信息和成员负责人维护接口，具体业务规则由 TeamService 统一处理。</p>
 */
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<PageResult<TeamVO>> search(@RequestBody(required = false) TeamSearchRequest request) {
        return ApiResponse.success(teamService.search(request));
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TeamVO> create(@Valid @RequestBody TeamCreateRequest request,
                                      @AuthenticationPrincipal CurrentUser currentUser,
                                      HttpServletRequest servletRequest) {
        return ApiResponse.success(teamService.create(
                request,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/{id}/detail")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TeamVO> detail(@PathVariable String id) {
        return ApiResponse.success(teamService.detail(id));
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("hasRole('ADMIN')")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TeamVO> update(@PathVariable String id,
                                      @Valid @RequestBody TeamUpdateRequest request,
                                      @AuthenticationPrincipal CurrentUser currentUser,
                                      HttpServletRequest servletRequest) {
        return ApiResponse.success(teamService.update(
                id,
                request,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<Void> delete(@PathVariable String id,
                                    @AuthenticationPrincipal CurrentUser currentUser,
                                    HttpServletRequest servletRequest) {
        teamService.delete(
                id,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );
        return ApiResponse.success();
    }

    @PostMapping("/{id}/members/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<PageResult<TeamMemberVO>> searchMembers(@PathVariable String id,
                                                               @RequestBody(required = false) TeamMemberSearchRequest request,
                                                               @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(teamService.searchMembers(id, request, currentUser));
    }

    @PostMapping("/{id}/members/update")
    @PreAuthorize("hasRole('ADMIN')")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TeamVO> updateMembers(@PathVariable String id,
                                             @Valid @RequestBody TeamMemberUpdateRequest request,
                                             @AuthenticationPrincipal CurrentUser currentUser,
                                             HttpServletRequest servletRequest) {
        return ApiResponse.success(teamService.updateMembers(
                id,
                request,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/{id}/leaders/update")
    @PreAuthorize("hasRole('ADMIN')")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TeamVO> updateLeaders(@PathVariable String id,
                                             @Valid @RequestBody TeamLeaderUpdateRequest request,
                                             @AuthenticationPrincipal CurrentUser currentUser,
                                             HttpServletRequest servletRequest) {
        return ApiResponse.success(teamService.updateLeaders(
                id,
                request,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));
    }
}
