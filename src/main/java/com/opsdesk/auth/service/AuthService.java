package com.opsdesk.auth.service;

import com.opsdesk.auth.dto.KickoutOthersRequest;
import com.opsdesk.auth.dto.LoginRequest;
import com.opsdesk.auth.dto.PasswordChangeRequest;
import com.opsdesk.auth.dto.RefreshTokenRequest;
import com.opsdesk.auth.dto.RegisterRequest;
import com.opsdesk.auth.dto.SmsCodeSendRequest;
import com.opsdesk.auth.vo.KickoutOthersVO;
import com.opsdesk.auth.vo.LoginResultVO;
import com.opsdesk.auth.vo.SmsCodeSendVO;
import com.opsdesk.user.vo.UserVO;

/**
 * 认证业务服务。
 *
 * <p>统一编排注册、登录、刷新、退出、当前用户和密码修改等认证相关流程。</p>
 */
public interface AuthService {

    UserVO register(RegisterRequest request, String requestIp, String userAgent);

    LoginResultVO login(LoginRequest request, String requestIp, String userAgent);

    LoginResultVO refresh(RefreshTokenRequest request);

    void logout(Long currentUserId, String accessToken, String requestIp, String userAgent);

    UserVO me(Long currentUserId);

    void changePassword(Long currentUserId, PasswordChangeRequest request, String requestIp, String userAgent);

    KickoutOthersVO kickoutOthers(Long currentUserId, KickoutOthersRequest request, String requestIp, String userAgent);

    SmsCodeSendVO sendSmsCode(SmsCodeSendRequest request);
}
