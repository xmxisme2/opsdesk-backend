package com.opsdesk.role.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.util.IdParser;
import com.opsdesk.permission.entity.Permission;
import com.opsdesk.permission.mapper.PermissionMapper;
import com.opsdesk.permission.service.PermissionCacheService;
import com.opsdesk.role.converter.RoleConverter;
import com.opsdesk.role.dto.RoleCreateRequest;
import com.opsdesk.role.dto.RolePermissionUpdateRequest;
import com.opsdesk.role.dto.RoleSearchRequest;
import com.opsdesk.role.dto.RoleUpdateRequest;
import com.opsdesk.role.entity.Role;
import com.opsdesk.role.mapper.RoleMapper;
import com.opsdesk.role.mapper.RolePermissionMapper;
import com.opsdesk.role.service.RoleService;
import com.opsdesk.role.vo.RoleVO;
import com.opsdesk.user.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 角色服务实现。
 *
 * <p>集中处理角色编码唯一性、内置角色保护、角色权限整体替换、审计日志和权限缓存失效。</p>
 */
@Service
public class RoleServiceImpl implements RoleService {

    /** 角色编码格式：必须以大写字母开头，只允许大写字母、数字和下划线，避免权限表达出现歧义。 */
    private static final Pattern ROLE_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,63}$");

    /** 管理员内置角色编码：用于保护管理员角色至少保留一个权限。 */
    private static final String ADMIN_ROLE_CODE = "ADMIN";

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleConverter roleConverter;
    private final SnowflakeIdGenerator idGenerator;
    private final PermissionCacheService permissionCacheService;
    private final AuditLogService auditLogService;

    public RoleServiceImpl(RoleMapper roleMapper,
                           PermissionMapper permissionMapper,
                           RolePermissionMapper rolePermissionMapper,
                           UserRoleMapper userRoleMapper,
                           RoleConverter roleConverter,
                           SnowflakeIdGenerator idGenerator,
                           PermissionCacheService permissionCacheService,
                           AuditLogService auditLogService) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleConverter = roleConverter;
        this.idGenerator = idGenerator;
        this.permissionCacheService = permissionCacheService;
        this.auditLogService = auditLogService;
    }

    @Override
    public PageResult<RoleVO> search(RoleSearchRequest request) {
        RoleSearchRequest safeRequest = request == null ? new RoleSearchRequest() : request;
        long page = safeRequest.normalizedPage();
        long size = safeRequest.normalizedSize();
        String keyword = safeRequest.normalizedKeyword();
        Integer enabled = safeRequest.getEnabled() == null ? null : (safeRequest.getEnabled() ? 1 : 0);

        long total = roleMapper.countSearch(keyword, enabled);
        if (total == 0) {
            return PageResult.empty(page, size);
        }
        long offset = (page - 1) * size;
        List<RoleVO> records = roleMapper.search(keyword, enabled, offset, size)
                .stream()
                .map(role -> roleConverter.toVO(role, permissionMapper.findIdsByRoleId(role.getId())))
                .toList();
        return new PageResult<>(records, page, size, total);
    }

    @Override
    @Transactional
    public RoleVO create(RoleCreateRequest request, Long operatorId, String requestIp, String userAgent) {
        String code = normalizeRoleCode(request.getCode());
        if (roleMapper.findByCode(code) != null) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "角色编码已存在");
        }

        Role role = new Role();
        role.setId(idGenerator.nextId());
        role.setCode(code);
        role.setName(request.getName().trim());
        role.setDescription(trimToNull(request.getDescription()));
        role.setBuiltIn(0);
        role.setEnabled(request.getEnabled() == null || request.getEnabled() ? 1 : 0);
        role.setCreateBy(operatorId);
        role.setUpdateBy(operatorId);
        roleMapper.insert(role);

        replaceRolePermissions(role, request.getPermissionIds(), operatorId);
        auditLogService.record(operatorId, "ROLE_CREATE", "ROLE", role.getId(),
                "创建角色：" + role.getCode(), requestIp, userAgent);
        return detail(String.valueOf(role.getId()));
    }

    @Override
    public RoleVO detail(String id) {
        Long roleId = IdParser.parseRequired(id, "角色ID");
        Role role = loadRole(roleId);
        return roleConverter.toVO(role, permissionMapper.findIdsByRoleId(roleId));
    }

    @Override
    @Transactional
    public RoleVO update(String id, RoleUpdateRequest request, Long operatorId, String requestIp, String userAgent) {
        Long roleId = IdParser.parseRequired(id, "角色ID");
        Role role = loadRole(roleId);
        if (isBuiltIn(role) && Boolean.FALSE.equals(request.getEnabled())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "内置角色不允许停用");
        }

        role.setName(request.getName().trim());
        role.setDescription(trimToNull(request.getDescription()));
        if (request.getEnabled() != null) {
            role.setEnabled(request.getEnabled() ? 1 : 0);
        }
        role.setUpdateBy(operatorId);
        roleMapper.update(role);

        if (request.getPermissionIds() != null) {
            replaceRolePermissions(role, request.getPermissionIds(), operatorId);
        }
        evictRoleUsers(roleId);
        auditLogService.record(operatorId, "ROLE_UPDATE", "ROLE", roleId,
                "编辑角色：" + role.getCode(), requestIp, userAgent);
        return detail(id);
    }

    @Override
    @Transactional
    public void delete(String id, Long operatorId, String requestIp, String userAgent) {
        Long roleId = IdParser.parseRequired(id, "角色ID");
        Role role = loadRole(roleId);
        if (isBuiltIn(role)) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "内置角色不允许删除");
        }
        if (userRoleMapper.countActiveByRoleId(roleId) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "角色仍有关联用户，不能删除");
        }
        rolePermissionMapper.deactivateMissing(roleId, List.of(), operatorId);
        roleMapper.logicalDelete(roleId, operatorId);
        auditLogService.record(operatorId, "ROLE_DELETE", "ROLE", roleId,
                "删除角色：" + role.getCode(), requestIp, userAgent);
    }

    @Override
    @Transactional
    public RoleVO updatePermissions(String id,
                                    RolePermissionUpdateRequest request,
                                    Long operatorId,
                                    String requestIp,
                                    String userAgent) {
        Long roleId = IdParser.parseRequired(id, "角色ID");
        Role role = loadRole(roleId);
        replaceRolePermissions(role, request.getPermissionIds(), operatorId);
        evictRoleUsers(roleId);
        auditLogService.record(operatorId, "ROLE_PERMISSION_UPDATE", "ROLE", roleId,
                "更新角色权限：" + role.getCode(), requestIp, userAgent);
        return detail(id);
    }

    private Role loadRole(Long roleId) {
        Role role = roleMapper.findById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private void replaceRolePermissions(Role role, List<String> permissionIdValues, Long operatorId) {
        List<Long> permissionIds = IdParser.parseDistinctList(permissionIdValues, "权限ID");
        if (ADMIN_ROLE_CODE.equals(role.getCode()) && permissionIds.isEmpty()) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "管理员角色至少保留一个权限");
        }
        validatePermissions(permissionIds);
        rolePermissionMapper.deactivateMissing(role.getId(), permissionIds, operatorId);
        for (Long permissionId : permissionIds) {
            if (rolePermissionMapper.countActive(role.getId(), permissionId) > 0) {
                continue;
            }
            int restored = rolePermissionMapper.restoreDeleted(role.getId(), permissionId, operatorId);
            if (restored == 0) {
                rolePermissionMapper.insert(idGenerator.nextId(), role.getId(), permissionId, operatorId);
            }
        }
    }

    private void validatePermissions(List<Long> permissionIds) {
        if (permissionIds.isEmpty()) {
            return;
        }
        List<Permission> permissions = permissionMapper.findEnabledByIds(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "权限不存在或已停用");
        }
    }

    private void evictRoleUsers(Long roleId) {
        permissionCacheService.evictUserPermissions(userRoleMapper.findUserIdsByRoleId(roleId));
    }

    private String normalizeRoleCode(String code) {
        String normalizedCode = code == null ? "" : code.trim().toUpperCase();
        if (!ROLE_CODE_PATTERN.matcher(normalizedCode).matches()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色编码只能包含大写字母、数字和下划线，且必须以字母开头");
        }
        return normalizedCode;
    }

    private boolean isBuiltIn(Role role) {
        return role.getBuiltIn() != null && role.getBuiltIn() == 1;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
