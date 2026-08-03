package com.opsdesk.ai.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 仅处理 /internal 路径的 AI 服务身份认证过滤器。 */
@Component
public class InboundServiceJwtAuthenticationFilter extends OncePerRequestFilter {
    private final InboundServiceJwtVerifier verifier;
    private final ObjectMapper objectMapper;

    public InboundServiceJwtAuthenticationFilter(InboundServiceJwtVerifier verifier, ObjectMapper objectMapper) {
        this.verifier = verifier;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            String token = StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")
                    ? authorization.substring(7) : null;
            verifier.verify(token);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "opsdesk-ai-service", null, List.of(new SimpleGrantedAuthority("ROLE_SERVICE")))
            );
            filterChain.doFilter(request, response);
        } catch (BusinessException exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_OK);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.error(exception.getErrorCode(), exception.getMessage()));
        }
    }
}
