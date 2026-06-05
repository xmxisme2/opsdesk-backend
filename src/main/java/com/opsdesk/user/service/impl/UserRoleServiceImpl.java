package com.opsdesk.user.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.util.IdParser;
import com.opsdesk.permission.service.PermissionCacheService;
import com.opsdesk.role.entity.Role;
import com.opsdesk.role.mapper.RoleMapper;
import com.opsdesk.user.dto.UserRoleUpdateRequest;
import com.opsdesk.user.entity.SysUser;
import com.opsdesk.user.mapper.SysUserMapper;
import com.opsdesk.user.mapper.UserRoleMapper;
import com.opsdesk.user.service.UserContextService;
import com.opsdesk.user.service.UserRoleService;
import com.opsdesk.user.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户角色服务实现。
 *
 * <p>集中处理后台用户角色整体替换、角色有效性校验、权限缓存清理和审计日志。</p>
 */
@Service
public class UserRoleServiceImpl implements UserRoleService {

    private final SysUserMapper sysUserMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserContextService userContextService;
    private final PermissionCacheService permissionCacheService;
    private final SnowflakeIdGenerator idGenerator;
    private final AuditLogService auditLogService;

    public UserRoleServiceImpl(SysUserMapper sysUserMapper,
                               RoleMapper roleMapper,
                               UserRoleMapper userRoleMapper,
                               UserContextService userContextService,
                               PermissionCacheService permissionCacheService,
                               SnowflakeIdGenerator idGenerator,
                               AuditLogService auditLogService) {
        this.sysUserMapper = sysUserMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.userContextService = userContextService;
        this.permissionCacheService = permissionCacheService;
        this.idGenerator = idGenerator;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public UserVO updateUserRoles(String userIdValue,
                                  UserRoleUpdateRequest request,
                                  Long operatorId,
                                  String requestIp,
                                  String userAgent) {
        Long userId = IdParser.parseRequired(userIdValue, "用户ID");
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        List<Long> roleIds = IdParser.parseDistinctList(request.getRoleIds(), "角色ID");
        if (roleIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色列表不能为空");
        }
        List<Role> roles = roleMapper.findEnabledByIds(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色不存在或已停用");
        }

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

        permissionCacheService.evictUserPermission(userId);
        auditLogService.record(operatorId, "USER_ROLE_UPDATE", "USER", userId,
                "更新用户角色：" + user.getUsername(), requestIp, userAgent);
        return userContextService.loadUserVO(userId);
    }
}
