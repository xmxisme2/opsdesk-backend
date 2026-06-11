package com.opsdesk.user.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.auth.service.TokenService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.department.mapper.DepartmentMapper;
import com.opsdesk.permission.mapper.PermissionMapper;
import com.opsdesk.permission.service.PermissionCacheService;
import com.opsdesk.role.mapper.RoleMapper;
import com.opsdesk.user.converter.UserConverter;
import com.opsdesk.user.dto.UserCreateRequest;
import com.opsdesk.user.dto.UserResetPasswordRequest;
import com.opsdesk.user.dto.UserStatusUpdateRequest;
import com.opsdesk.user.entity.SysUser;
import com.opsdesk.user.mapper.SysUserMapper;
import com.opsdesk.user.mapper.UserRoleMapper;
import com.opsdesk.user.vo.UserResetPasswordVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户管理服务单元测试。
 *
 * <p>覆盖后台用户 CRUD 中的唯一性、自我停用保护和管理员重置密码规则，避免管理页操作破坏登录安全。</p>
 */
@ExtendWith(MockitoExtension.class)
class UserManagementServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private PermissionCacheService permissionCacheService;

    @Mock
    private TokenService tokenService;

    @Mock
    private AuditLogService auditLogService;

    private BCryptPasswordEncoder passwordEncoder;
    private UserManagementServiceImpl userManagementService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userManagementService = new UserManagementServiceImpl(
                sysUserMapper,
                userRoleMapper,
                departmentMapper,
                roleMapper,
                permissionMapper,
                new UserConverter(),
                permissionCacheService,
                tokenService,
                passwordEncoder,
                new SnowflakeIdGenerator(),
                auditLogService
        );
    }

    @Test
    void createShouldRejectDuplicatedPhoneBeforeInsert() {
        UserCreateRequest request = new UserCreateRequest();
        request.setPhone("13900000000");
        request.setUsername("zhangsan");
        request.setPassword("password123");
        request.setNickname("张三");
        request.setDepartmentId("1");
        request.setRoleIds(java.util.List.of("4"));

        when(sysUserMapper.countByPhone("13900000000")).thenReturn(1);

        assertThatThrownBy(() -> userManagementService.create(request, 1L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);

        verify(sysUserMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateStatusShouldRejectSelfDisable() {
        UserStatusUpdateRequest request = new UserStatusUpdateRequest();
        request.setStatus("DISABLED");

        assertThatThrownBy(() -> userManagementService.updateStatus("1", request, 1L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);

        verify(sysUserMapper, never()).updateStatus(eq(1L), eq("DISABLED"), eq(1L));
    }

    @Test
    void deleteShouldRejectSelfDelete() {
        assertThatThrownBy(() -> userManagementService.delete("1", 1L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);

        verify(sysUserMapper, never()).logicalDelete(eq(1L), eq("DISABLED"), eq(1L));
    }

    @Test
    void resetPasswordShouldGenerateTemporaryPasswordAndInvalidateSessions() {
        SysUser user = new SysUser();
        user.setId(2L);
        user.setPhone("13900000000");
        user.setUsername("zhangsan");
        when(sysUserMapper.findById(2L)).thenReturn(user);
        when(sysUserMapper.updatePassword(eq(2L), org.mockito.ArgumentMatchers.anyString(), eq(1L))).thenReturn(1);

        UserResetPasswordVO result = userManagementService.resetPassword(
                "2",
                new UserResetPasswordRequest(),
                1L,
                "127.0.0.1",
                "JUnit"
        );

        assertThat(result.temporaryPassword()).isNotBlank();

        ArgumentCaptor<String> passwordHashCaptor = ArgumentCaptor.forClass(String.class);
        verify(sysUserMapper).updatePassword(eq(2L), passwordHashCaptor.capture(), eq(1L));
        assertThat(passwordEncoder.matches(result.temporaryPassword(), passwordHashCaptor.getValue())).isTrue();
        verify(tokenService).invalidateAllRefreshSessions(2L);
    }
}
