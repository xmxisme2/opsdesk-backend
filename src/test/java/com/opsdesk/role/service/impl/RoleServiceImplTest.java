package com.opsdesk.role.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.permission.mapper.PermissionMapper;
import com.opsdesk.permission.service.PermissionCacheService;
import com.opsdesk.role.converter.RoleConverter;
import com.opsdesk.role.dto.RoleCreateRequest;
import com.opsdesk.role.entity.Role;
import com.opsdesk.role.mapper.RoleMapper;
import com.opsdesk.role.mapper.RolePermissionMapper;
import com.opsdesk.user.mapper.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 角色服务单元测试。
 *
 * <p>优先覆盖角色权限模块的保护性业务规则，避免内置角色和唯一编码被后续改动破坏。</p>
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private RolePermissionMapper rolePermissionMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private PermissionCacheService permissionCacheService;

    @Mock
    private AuditLogService auditLogService;

    private RoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleServiceImpl(
                roleMapper,
                permissionMapper,
                rolePermissionMapper,
                userRoleMapper,
                new RoleConverter(),
                new SnowflakeIdGenerator(),
                permissionCacheService,
                auditLogService
        );
    }

    @Test
    void createShouldRejectDuplicatedRoleCode() {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setCode("ops_admin");
        request.setName("运维管理员");

        when(roleMapper.findByCode("OPS_ADMIN")).thenReturn(new Role());

        assertThatThrownBy(() -> roleService.create(request, 1L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);
    }

    @Test
    void deleteShouldRejectBuiltInRole() {
        Role role = new Role();
        role.setId(1L);
        role.setCode("ADMIN");
        role.setBuiltIn(1);
        when(roleMapper.findById(1L)).thenReturn(role);

        assertThatThrownBy(() -> roleService.delete("1", 1L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);

        verify(roleMapper, never()).logicalDelete(1L, 1L);
    }
}
