package com.opsdesk.auth.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.auth.dto.KickoutOthersRequest;
import com.opsdesk.auth.dto.LoginRequest;
import com.opsdesk.auth.dto.PasswordChangeRequest;
import com.opsdesk.auth.dto.RefreshTokenRequest;
import com.opsdesk.auth.dto.RegisterRequest;
import com.opsdesk.auth.dto.SmsCodeSendRequest;
import com.opsdesk.auth.model.TokenPair;
import com.opsdesk.auth.service.AuthService;
import com.opsdesk.auth.service.CaptchaService;
import com.opsdesk.auth.service.TokenService;
import com.opsdesk.auth.service.SmsVerificationService;
import com.opsdesk.auth.vo.KickoutOthersVO;
import com.opsdesk.auth.vo.LoginResultVO;
import com.opsdesk.auth.vo.SmsCodeSendVO;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.department.entity.Department;
import com.opsdesk.department.mapper.DepartmentMapper;
import com.opsdesk.role.entity.Role;
import com.opsdesk.role.mapper.RoleMapper;
import com.opsdesk.user.entity.SysUser;
import com.opsdesk.user.mapper.SysUserMapper;
import com.opsdesk.user.mapper.UserRoleMapper;
import com.opsdesk.user.service.UserContextService;
import com.opsdesk.user.vo.UserVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 认证业务服务实现。
 *
 * <p>Controller 只负责收参和返回，本类集中处理注册、登录、令牌、密码和审计日志。</p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    /** 新注册用户默认角色编码：注册成功后自动绑定 USER 角色。 */
    private static final String DEFAULT_ROLE_CODE = "USER";

    /** 用户正常状态编码：只有 ACTIVE 账号允许登录和继续访问业务接口。 */
    private static final String STATUS_ACTIVE = "ACTIVE";

    /** 用户审计业务类型：注册、登录、退出、改密等用户操作统一归入 USER。 */
    private static final String BIZ_TYPE_USER = "USER";

    private final SysUserMapper sysUserMapper;
    private final UserRoleMapper userRoleMapper;
    private final DepartmentMapper departmentMapper;
    private final RoleMapper roleMapper;
    private final UserContextService userContextService;
    private final CaptchaService captchaService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdGenerator idGenerator;
    private final AuditLogService auditLogService;
    private final SmsVerificationService smsVerificationService;

    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           UserRoleMapper userRoleMapper,
                           DepartmentMapper departmentMapper,
                           RoleMapper roleMapper,
                           UserContextService userContextService,
                           CaptchaService captchaService,
                           TokenService tokenService,
                           PasswordEncoder passwordEncoder,
                           SnowflakeIdGenerator idGenerator,
                           AuditLogService auditLogService,
                           SmsVerificationService smsVerificationService) {
        this.sysUserMapper = sysUserMapper;
        this.userRoleMapper = userRoleMapper;
        this.departmentMapper = departmentMapper;
        this.roleMapper = roleMapper;
        this.userContextService = userContextService;
        this.captchaService = captchaService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.idGenerator = idGenerator;
        this.auditLogService = auditLogService;
        this.smsVerificationService = smsVerificationService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterRequest request, String requestIp, String userAgent) {
        validateOptionalCaptcha(request.getCaptchaId(), request.getCaptchaCode());
        Long departmentId = parseId(request.getDepartmentId(), "部门 ID 格式错误");
        Department department = departmentMapper.findEnabledById(departmentId);
        if (department == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "部门不存在或已停用");
        }
        if (sysUserMapper.countByPhone(request.getPhone()) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "手机号已注册");
        }

        Role defaultRole = roleMapper.findEnabledByCode(DEFAULT_ROLE_CODE);
        if (defaultRole == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "默认 USER 角色未初始化");
        }

        long userId = idGenerator.nextId();
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setUsername(request.getPhone());
        user.setNickname(resolveNickname(request));
        user.setEmail(request.getEmail());
        user.setGender(resolveGender(request.getGender()));
        user.setAvatarCode(resolveAvatarCode(user.getGender(), request.getAvatarCode()));
        user.setAvatarUrl(resolveAvatarUrl(user.getAvatarCode()));
        user.setDepartmentId(departmentId);
        user.setStatus(STATUS_ACTIVE);
        user.setCreateBy(userId);
        user.setUpdateBy(userId);

        try {
            sysUserMapper.insert(user);
            userRoleMapper.insert(idGenerator.nextId(), userId, defaultRole.getId(), userId);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "手机号已注册");
        }

        auditLogService.record(userId, "USER_REGISTER", BIZ_TYPE_USER, userId, "用户完成手机号注册", requestIp, userAgent);
        return userContextService.loadUserVO(userId);
    }

    @Override
    public LoginResultVO login(LoginRequest request, String requestIp, String userAgent) {
        if (!"IMAGE".equalsIgnoreCase(request.getCaptchaType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "首版仅支持图形验证码登录");
        }
        captchaService.validate(request.getCaptchaId(), request.getCaptchaCode());

        SysUser user = sysUserMapper.findByPhone(request.getPhone());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号或密码错误");
        }
        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已停用或锁定");
        }

        TokenPair tokenPair = tokenService.issueTokenPair(user.getId(), request.isRememberMe());
        UserVO userVO = userContextService.loadUserVO(user.getId());
        auditLogService.record(user.getId(), "USER_LOGIN", BIZ_TYPE_USER, user.getId(), "用户登录成功", requestIp, userAgent);
        return toLoginResult(tokenPair, userVO);
    }

    @Override
    public LoginResultVO refresh(RefreshTokenRequest request) {
        TokenPair tokenPair = tokenService.refresh(request.getRefreshToken());
        Long userId = tokenService.parseAccessToken(tokenPair.accessToken()).userId();
        UserVO userVO = userContextService.loadUserVO(userId);
        return toLoginResult(tokenPair, userVO);
    }

    @Override
    public void logout(Long currentUserId, String accessToken, String requestIp, String userAgent) {
        tokenService.logoutAccessToken(accessToken);
        auditLogService.record(currentUserId, "USER_LOGOUT", BIZ_TYPE_USER, currentUserId, "用户退出登录", requestIp, userAgent);
    }

    @Override
    public UserVO me(Long currentUserId) {
        return userContextService.loadUserVO(currentUserId);
    }

    @Override
    public void changePassword(Long currentUserId, PasswordChangeRequest request, String requestIp, String userAgent) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的新密码不一致");
        }

        SysUser user = sysUserMapper.findById(currentUserId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "旧密码错误");
        }

        sysUserMapper.updatePassword(currentUserId, passwordEncoder.encode(request.getNewPassword()), currentUserId);
        if (request.isKickoutOthers()) {
            tokenService.invalidateAllRefreshSessions(currentUserId);
        }
        auditLogService.record(currentUserId, "PASSWORD_CHANGE", BIZ_TYPE_USER, currentUserId, "用户修改登录密码", requestIp, userAgent);
    }

    @Override
    public KickoutOthersVO kickoutOthers(Long currentUserId,
                                         KickoutOthersRequest request,
                                         String requestIp,
                                         String userAgent) {
        int kickedCount = tokenService.kickoutOtherSessions(currentUserId, request.getCurrentRefreshToken());
        auditLogService.record(currentUserId, "KICKOUT_SESSIONS", BIZ_TYPE_USER, currentUserId, "用户踢出其他登录会话", requestIp, userAgent);
        return new KickoutOthersVO(kickedCount);
    }

    @Override
    public SmsCodeSendVO sendSmsCode(SmsCodeSendRequest request) {
        smsVerificationService.send(request.getPhone(), request.getScene());
        return new SmsCodeSendVO(true, "验证码已发送，请注意查收");
    }

    private void validateOptionalCaptcha(String captchaId, String captchaCode) {
        if (StringUtils.hasText(captchaId) || StringUtils.hasText(captchaCode)) {
            captchaService.validate(captchaId, captchaCode);
        }
    }

    private LoginResultVO toLoginResult(TokenPair tokenPair, UserVO userVO) {
        return new LoginResultVO(
                tokenPair.accessToken(),
                "Bearer",
                tokenPair.expiresIn(),
                tokenPair.refreshToken(),
                tokenPair.refreshExpiresIn(),
                userVO
        );
    }

    private Long parseId(String value, String message) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }

    private String resolveNickname(RegisterRequest request) {
        if (StringUtils.hasText(request.getNickname())) {
            return request.getNickname().trim();
        }
        return "用户" + request.getPhone().substring(request.getPhone().length() - 4);
    }

    private String resolveGender(String gender) {
        if (!StringUtils.hasText(gender)) {
            return "MALE";
        }
        if (!"MALE".equalsIgnoreCase(gender) && !"FEMALE".equalsIgnoreCase(gender)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "性别只能是 MALE 或 FEMALE");
        }
        return gender.toUpperCase();
    }

    private String resolveAvatarCode(String gender, String avatarCode) {
        if (StringUtils.hasText(avatarCode)) {
            return avatarCode;
        }
        return "FEMALE".equals(gender) ? "avatar_female_01" : "avatar_male_01";
    }

    private String resolveAvatarUrl(String avatarCode) {
        return "/assets/avatars/" + avatarCode + ".png";
    }
}
