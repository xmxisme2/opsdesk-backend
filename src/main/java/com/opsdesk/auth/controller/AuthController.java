package com.opsdesk.auth.controller;

import com.opsdesk.auth.dto.CaptchaRequest;
import com.opsdesk.auth.dto.KickoutOthersRequest;
import com.opsdesk.auth.dto.LoginRequest;
import com.opsdesk.auth.dto.PasswordChangeRequest;
import com.opsdesk.auth.dto.RefreshTokenRequest;
import com.opsdesk.auth.dto.RegisterRequest;
import com.opsdesk.auth.dto.SmsCodeSendRequest;
import com.opsdesk.auth.service.AuthService;
import com.opsdesk.auth.service.CaptchaService;
import com.opsdesk.auth.vo.CaptchaVO;
import com.opsdesk.auth.vo.KickoutOthersVO;
import com.opsdesk.auth.vo.LoginResultVO;
import com.opsdesk.auth.vo.SmsCodeSendVO;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.user.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口 Controller。
 *
 * <p>提供注册、登录、验证码、刷新、退出、当前用户、改密和多端踢出接口。</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    public AuthController(AuthService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    @PostMapping("/captcha")
    public ApiResponse<CaptchaVO> captcha(@RequestBody(required = false) CaptchaRequest request) {
        CaptchaRequest actualRequest = request == null ? new CaptchaRequest() : request;
        return ApiResponse.success(captchaService.createCaptcha(actualRequest));
    }

    @PostMapping("/sms-code/send")
    public ApiResponse<SmsCodeSendVO> sendSmsCode(@Valid @RequestBody SmsCodeSendRequest request) {
        return ApiResponse.success(authService.sendSmsCode(request));
    }

    @PostMapping("/register")
    public ApiResponse<UserVO> register(@Valid @RequestBody RegisterRequest request,
                                        HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.register(request, clientIp(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResultVO> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.login(request, clientIp(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResultVO> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal CurrentUser currentUser,
                                    HttpServletRequest servletRequest) {
        authService.logout(
                currentUser.getUserId(),
                bearerToken(servletRequest),
                clientIp(servletRequest),
                userAgent(servletRequest)
        );
        return ApiResponse.success();
    }

    @PostMapping("/me")
    public ApiResponse<UserVO> me(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(authService.me(currentUser.getUserId()));
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal CurrentUser currentUser,
                                            @Valid @RequestBody PasswordChangeRequest request,
                                            HttpServletRequest servletRequest) {
        authService.changePassword(currentUser.getUserId(), request, clientIp(servletRequest), userAgent(servletRequest));
        return ApiResponse.success();
    }

    @PostMapping("/sessions/kickout-others")
    public ApiResponse<KickoutOthersVO> kickoutOthers(@AuthenticationPrincipal CurrentUser currentUser,
                                                      @Valid @RequestBody KickoutOthersRequest request,
                                                      HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.kickoutOthers(
                currentUser.getUserId(),
                request,
                clientIp(servletRequest),
                userAgent(servletRequest)
        ));
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return "";
        }
        return authorization.substring(7);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
