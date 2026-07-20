package com.opsdesk.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsdesk.auth.model.TokenClaims;
import com.opsdesk.auth.service.TokenService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.user.service.UserContextService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * JWT 认证过滤器。
 *
 * <p>解析 Bearer access token，校验黑名单和过期时间后注入当前用户上下文。</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 精确匹配的公开接口路径：这些入口不需要 JWT，主要用于健康检查、认证入口和公开头像选项。 */
    private static final Set<String> PUBLIC_EXACT_PATHS = Set.of(
            "/api/health/check",
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/captcha",
            "/api/auth/sms-code/send",
            "/api/auth/refresh",
            "/api/users/avatar-options",
            "/swagger-ui.html"
    );

    private final TokenService tokenService;
    private final UserContextService userContextService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(TokenService tokenService,
                                   UserContextService userContextService,
                                   ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.userContextService = userContextService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return PUBLIC_EXACT_PATHS.contains(path)
                || path.startsWith("/uploads/avatars/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveBearerToken(request);
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            TokenClaims claims = tokenService.parseAccessToken(token);
            CurrentUser currentUser = userContextService.loadCurrentUser(claims.userId());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    currentUser,
                    null,
                    currentUser.toAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (BusinessException exception) {
            SecurityContextHolder.clearContext();
            writeBusinessError(response, exception);
        }
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }

    private void writeBusinessError(HttpServletResponse response, BusinessException exception) throws IOException {
        ErrorCode errorCode = exception.getErrorCode() == ErrorCode.FORBIDDEN
                ? ErrorCode.FORBIDDEN
                : ErrorCode.UNAUTHORIZED;
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode, exception.getMessage()));
    }
}
