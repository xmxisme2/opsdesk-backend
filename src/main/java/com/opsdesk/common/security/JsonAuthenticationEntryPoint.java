package com.opsdesk.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security 未登录响应处理器。
 *
 * <p>业务接口缺少或携带无效 token 时统一返回契约约定的 401001。</p>
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        write(response, ApiResponse.error(ErrorCode.UNAUTHORIZED));
    }

    public void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        write(response, ApiResponse.error(ErrorCode.UNAUTHORIZED, message));
    }

    private void write(HttpServletResponse response, ApiResponse<Void> body) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
