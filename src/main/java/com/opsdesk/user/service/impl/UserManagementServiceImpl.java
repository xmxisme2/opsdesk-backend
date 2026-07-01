package com.opsdesk.user.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.auth.service.TokenService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.pagination.PageHelperPageResult;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.util.IdParser;
import com.opsdesk.department.entity.Department;
import com.opsdesk.department.mapper.DepartmentMapper;
import com.opsdesk.permission.entity.Permission;
import com.opsdesk.permission.mapper.PermissionMapper;
import com.opsdesk.permission.service.PermissionCacheService;
import com.opsdesk.role.entity.Role;
import com.opsdesk.role.mapper.RoleMapper;
import com.opsdesk.user.converter.UserConverter;
import com.opsdesk.user.dto.UserCreateRequest;
import com.opsdesk.user.dto.UserResetPasswordRequest;
import com.opsdesk.user.dto.UserSearchRequest;
import com.opsdesk.user.dto.UserStatusUpdateRequest;
import com.opsdesk.user.dto.UserUpdateRequest;
import com.opsdesk.user.entity.SysUser;
import com.opsdesk.user.mapper.SysUserMapper;
import com.opsdesk.user.mapper.UserRoleMapper;
import com.opsdesk.user.service.UserManagementService;
import com.opsdesk.user.vo.UserResetPasswordVO;
import com.opsdesk.user.vo.UserVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.List;

/**
 * 后台用户管理服务实现。
 *
 * <p>管理员 CRUD 与登录态加载分开实现：管理页允许查看停用和锁定账号，但安全上下文仍只允许 ACTIVE 用户访问业务接口。</p>
 */
@Service
public class UserManagementServiceImpl implements UserManagementService {

    /** 用户正常状态：允许登录和访问业务接口，可由管理端创建、编辑、启停接口传入。 */
    private static final String STATUS_ACTIVE = "ACTIVE";

    /** 用户停用状态：管理员主动停用账号，外部只能通过管理接口传入。 */
    private static final String STATUS_DISABLED = "DISABLED";

    /** 用户锁定状态：安全风控或管理员锁定账号，外部只能通过管理接口传入。 */
    private static final String STATUS_LOCKED = "LOCKED";

    /** 用户状态白名单：集中限制管理端可传入的账号状态，避免裸字符串扩散。 */
    private static final List<String> SUPPORTED_USER_STATUSES = List.of(STATUS_ACTIVE, STATUS_DISABLED, STATUS_LOCKED);

    /** 默认性别：创建用户未传 gender 时用于选择男士默认头像，可由外部传 MALE/FEMALE 覆盖。 */
    private static final String DEFAULT_GENDER = "MALE";

    /** 女士性别编码：用于校验外部 gender 入参和选择女士默认头像。 */
    private static final String GENDER_FEMALE = "FEMALE";

    /** 男士性别编码：用于校验外部 gender 入参和选择男士默认头像。 */
    private static final String GENDER_MALE = "MALE";

    /** 用户审计业务类型：用户创建、编辑、启停和重置密码统一归入 USER。 */
    private static final String BIZ_TYPE_USER = "USER";

    /** 创建用户审计操作类型：管理员新增账号时写入 audit_log，不允许外部传入。 */
    private static final String OPERATION_USER_CREATE = "USER_CREATE";

    /** 编辑用户审计操作类型：管理员保存账号资料时写入 audit_log，不允许外部传入。 */
    private static final String OPERATION_USER_UPDATE = "USER_UPDATE";

    /** 启停用户审计操作类型：管理员变更账号状态时写入 audit_log，不允许外部传入。 */
    private static final String OPERATION_USER_STATUS_UPDATE = "USER_STATUS_UPDATE";

    /** 删除用户审计操作类型：管理员逻辑删除账号时写入 audit_log，不允许外部传入。 */
    private static final String OPERATION_USER_DELETE = "USER_DELETE";

    /** 重置密码审计操作类型：管理员重置账号密码时写入 audit_log，不允许外部传入。 */
    private static final String OPERATION_USER_PASSWORD_RESET = "USER_PASSWORD_RESET";

    /** 女士默认头像编码：创建用户未传 avatarCode 且 gender=FEMALE 时使用。 */
    private static final String DEFAULT_FEMALE_AVATAR_CODE = "avatar_female_01";

    /** 男士默认头像编码：创建用户未传 avatarCode 或未传 gender 时使用。 */
    private static final String DEFAULT_MALE_AVATAR_CODE = "avatar_male_01";

    /** 默认头像访问路径前缀：根据头像编码组装前端可访问的资源路径，不允许外部传入。 */
    private static final String AVATAR_ASSET_PREFIX = "/assets/avatars/";

    /** 默认头像文件后缀：当前默认头像统一使用 png 静态资源，不允许外部传入。 */
    private static final String AVATAR_ASSET_SUFFIX = ".png";

    /** 临时密码字符集：管理员未指定新密码时使用，避免包含容易混淆的特殊字符。 */
    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    /** 临时密码长度：满足当前密码最小长度要求，并兼顾管理员口头转达成本。 */
    private static final int TEMP_PASSWORD_LENGTH = 12;

    /** 临时密码随机源：用于管理员重置密码时生成不可预测的一次性密码。 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SysUserMapper sysUserMapper;
    private final UserRoleMapper userRoleMapper;
    private final DepartmentMapper departmentMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserConverter userConverter;
    private final PermissionCacheService permissionCacheService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdGenerator idGenerator;
    private final AuditLogService auditLogService;

    public UserManagementServiceImpl(SysUserMapper sysUserMapper,
                                     UserRoleMapper userRoleMapper,
                                     DepartmentMapper departmentMapper,
                                     RoleMapper roleMapper,
                                     PermissionMapper permissionMapper,
                                     UserConverter userConverter,
                                     PermissionCacheService permissionCacheService,
                                     TokenService tokenService,
                                     PasswordEncoder passwordEncoder,
                                     SnowflakeIdGenerator idGenerator,
                                     AuditLogService auditLogService) {
        this.sysUserMapper = sysUserMapper;
        this.userRoleMapper = userRoleMapper;
        this.departmentMapper = departmentMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userConverter = userConverter;
        this.permissionCacheService = permissionCacheService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.idGenerator = idGenerator;
        this.auditLogService = auditLogService;
    }

    @Override
    public PageResult<UserVO> search(UserSearchRequest request) {
        UserSearchRequest safeRequest = request == null ? new UserSearchRequest() : request;
        Long departmentId = parseOptionalId(safeRequest.getDepartmentId(), "部门ID");
        String roleCode = safeRequest.normalizedRoleCode();
        String status = normalizeOptionalStatus(safeRequest.normalizedStatus());

        return PageHelperPageResult.selectPage(
                safeRequest,
                () -> sysUserMapper.search(safeRequest.normalizedKeyword(), departmentId, roleCode, status),
                this::assembleUserVO
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO create(UserCreateRequest request, Long operatorId, String requestIp, String userAgent) {
        String phone = request.getPhone().trim();
        String username = resolveUsername(request.getUsername(), phone);
        String status = normalizeStatusOrDefault(request.getStatus());
        String gender = resolveGender(request.getGender(), DEFAULT_GENDER);
        if (sysUserMapper.countByPhone(phone) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "手机号已存在");
        }
        if (sysUserMapper.countByUsername(username) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "用户名已存在");
        }
        Long departmentId = parseAndValidateDepartment(request.getDepartmentId());
        List<Long> roleIds = parseAndValidateRoles(request.getRoleIds());

        SysUser user = new SysUser();
        user.setId(idGenerator.nextId());
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setUsername(username);
        user.setNickname(request.getNickname().trim());
        user.setEmail(trimToNull(request.getEmail()));
        user.setGender(gender);
        user.setAvatarCode(resolveAvatarCode(gender, request.getAvatarCode()));
        user.setAvatarUrl(resolveAvatarUrl(user.getAvatarCode()));
        user.setDepartmentId(departmentId);
        user.setStatus(status);
        user.setCreateBy(operatorId);
        user.setUpdateBy(operatorId);

        try {
            sysUserMapper.insert(user);
            replaceUserRoles(user.getId(), roleIds, operatorId);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "手机号或用户名已存在");
        }

        permissionCacheService.evictUserPermission(user.getId());
        auditLogService.record(operatorId, OPERATION_USER_CREATE, BIZ_TYPE_USER, user.getId(),
                "后台创建用户：" + user.getUsername(), requestIp, userAgent);
        return detail(String.valueOf(user.getId()));
    }

    @Override
    public UserVO detail(String id) {
        Long userId = IdParser.parseRequired(id, "用户ID");
        return assembleUserVO(loadUser(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO update(String id, UserUpdateRequest request, Long operatorId, String requestIp, String userAgent) {
        Long userId = IdParser.parseRequired(id, "用户ID");
        SysUser user = loadUser(userId);

        String status = StringUtils.hasText(request.getStatus()) ? normalizeStatus(request.getStatus()) : user.getStatus();
        guardSelfInactiveStatus(userId, status, operatorId);
        String phone = StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : user.getPhone();
        if (sysUserMapper.countByPhoneExcludeId(phone, userId) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "手机号已存在");
        }

        user.setPhone(phone);
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : user.getNickname());
        user.setEmail(request.getEmail() == null ? user.getEmail() : trimToNull(request.getEmail()));
        user.setGender(StringUtils.hasText(request.getGender()) ? resolveGender(request.getGender(), user.getGender()) : user.getGender());
        user.setAvatarCode(StringUtils.hasText(request.getAvatarCode()) ? request.getAvatarCode().trim() : user.getAvatarCode());
        user.setAvatarUrl(resolveAvatarUrl(user.getAvatarCode()));
        user.setDepartmentId(StringUtils.hasText(request.getDepartmentId())
                ? parseAndValidateDepartment(request.getDepartmentId())
                : user.getDepartmentId());
        user.setStatus(status);
        user.setUpdateBy(operatorId);

        if (sysUserMapper.updateProfile(user) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (!STATUS_ACTIVE.equals(status)) {
            tokenService.invalidateAllRefreshSessions(userId);
        }
        permissionCacheService.evictUserPermission(userId);
        auditLogService.record(operatorId, OPERATION_USER_UPDATE, BIZ_TYPE_USER, userId,
                "编辑用户：" + user.getUsername(), requestIp, userAgent);
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateStatus(String id,
                               UserStatusUpdateRequest request,
                               Long operatorId,
                               String requestIp,
                               String userAgent) {
        Long userId = IdParser.parseRequired(id, "用户ID");
        String status = normalizeStatus(request.getStatus());
        guardSelfInactiveStatus(userId, status, operatorId);
        SysUser user = loadUser(userId);

        if (sysUserMapper.updateStatus(userId, status, operatorId) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (!STATUS_ACTIVE.equals(status) && shouldKickoutSessions(request.getKickoutSessions())) {
            tokenService.invalidateAllRefreshSessions(userId);
        }
        permissionCacheService.evictUserPermission(userId);
        auditLogService.record(operatorId, OPERATION_USER_STATUS_UPDATE, BIZ_TYPE_USER, userId,
                buildStatusAuditContent(user, status, request.getReason()), requestIp, userAgent);
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id, Long operatorId, String requestIp, String userAgent) {
        Long userId = IdParser.parseRequired(id, "用户ID");
        if (userId.equals(operatorId)) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "不能删除当前登录账号");
        }
        SysUser user = loadUser(userId);

        userRoleMapper.deactivateMissing(userId, List.of(), operatorId);
        if (sysUserMapper.logicalDelete(userId, STATUS_DISABLED, operatorId) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        tokenService.invalidateAllRefreshSessions(userId);
        permissionCacheService.evictUserPermission(userId);
        auditLogService.record(operatorId, OPERATION_USER_DELETE, BIZ_TYPE_USER, userId,
                "删除用户：" + user.getUsername(), requestIp, userAgent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResetPasswordVO resetPassword(String id,
                                             UserResetPasswordRequest request,
                                             Long operatorId,
                                             String requestIp,
                                             String userAgent) {
        Long userId = IdParser.parseRequired(id, "用户ID");
        SysUser user = loadUser(userId);
        String temporaryPassword = resolveTemporaryPassword(request);

        if (sysUserMapper.updatePassword(userId, passwordEncoder.encode(temporaryPassword), operatorId) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        tokenService.invalidateAllRefreshSessions(userId);
        auditLogService.record(operatorId, OPERATION_USER_PASSWORD_RESET, BIZ_TYPE_USER, userId,
                "管理员重置用户密码：" + user.getUsername(), requestIp, userAgent);
        return new UserResetPasswordVO(temporaryPassword);
    }

    private SysUser loadUser(Long userId) {
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private UserVO assembleUserVO(SysUser user) {
        Department department = user.getDepartmentId() == null ? null : departmentMapper.findEnabledById(user.getDepartmentId());
        List<Role> roles = roleMapper.findEnabledByUserId(user.getId());
        List<Permission> permissions = permissionMapper.findEnabledByUserId(user.getId());
        return userConverter.toVO(user, department, roles, permissions);
    }

    private void replaceUserRoles(Long userId, List<Long> roleIds, Long operatorId) {
        userRoleMapper.deactivateMissing(userId, roleIds, operatorId);
        for (Long roleId : roleIds) {
            if (userRoleMapper.countActive(userId, roleId) > 0) {
                continue;
            }
            int restored = userRoleMapper.restoreDeleted(userId, roleId, operatorId);
            if (restored == 0) {
                userRoleMapper.insert(idGenerator.nextId(), userId, roleId, operatorId);
            }
        }
    }

    private List<Long> parseAndValidateRoles(List<String> roleIdValues) {
        List<Long> roleIds = IdParser.parseDistinctList(roleIdValues, "角色ID");
        if (roleIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色列表不能为空");
        }
        List<Role> roles = roleMapper.findEnabledByIds(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色不存在或已停用");
        }
        return roleIds;
    }

    private Long parseAndValidateDepartment(String departmentIdValue) {
        Long departmentId = IdParser.parseRequired(departmentIdValue, "部门ID");
        if (departmentMapper.findEnabledById(departmentId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "部门不存在或已停用");
        }
        return departmentId;
    }

    private Long parseOptionalId(String value, String fieldName) {
        return StringUtils.hasText(value) ? IdParser.parseRequired(value, fieldName) : null;
    }

    private String normalizeStatusOrDefault(String status) {
        return StringUtils.hasText(status) ? normalizeStatus(status) : STATUS_ACTIVE;
    }

    private String normalizeOptionalStatus(String status) {
        return StringUtils.hasText(status) ? normalizeStatus(status) : null;
    }

    private String normalizeStatus(String status) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        if (!SUPPORTED_USER_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户状态只能是 ACTIVE、DISABLED 或 LOCKED");
        }
        return normalizedStatus;
    }

    private void guardSelfInactiveStatus(Long userId, String status, Long operatorId) {
        if (userId.equals(operatorId) && !STATUS_ACTIVE.equals(status)) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "不能停用或锁定当前登录账号");
        }
    }

    private String resolveUsername(String username, String phone) {
        if (!StringUtils.hasText(username)) {
            return phone;
        }
        return username.trim();
    }

    private String resolveGender(String gender, String defaultGender) {
        String resolvedGender = StringUtils.hasText(gender) ? gender.trim().toUpperCase() : defaultGender;
        if (!GENDER_MALE.equals(resolvedGender) && !GENDER_FEMALE.equals(resolvedGender)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "性别只能是 MALE 或 FEMALE");
        }
        return resolvedGender;
    }

    private String resolveAvatarCode(String gender, String avatarCode) {
        if (StringUtils.hasText(avatarCode)) {
            return avatarCode.trim();
        }
        return GENDER_FEMALE.equals(gender) ? DEFAULT_FEMALE_AVATAR_CODE : DEFAULT_MALE_AVATAR_CODE;
    }

    private String resolveAvatarUrl(String avatarCode) {
        return StringUtils.hasText(avatarCode) ? AVATAR_ASSET_PREFIX + avatarCode + AVATAR_ASSET_SUFFIX : null;
    }

    private String resolveTemporaryPassword(UserResetPasswordRequest request) {
        if (request != null && StringUtils.hasText(request.getNewPassword())) {
            return request.getNewPassword();
        }
        StringBuilder password = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            password.append(TEMP_PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

    private boolean shouldKickoutSessions(Boolean kickoutSessions) {
        return kickoutSessions == null || kickoutSessions;
    }

    private String buildStatusAuditContent(SysUser user, String status, String reason) {
        String content = "更新用户状态：" + user.getUsername() + " -> " + status;
        return StringUtils.hasText(reason) ? content + "，原因：" + reason.trim() : content;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
