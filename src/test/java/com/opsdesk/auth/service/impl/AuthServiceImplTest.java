package com.opsdesk.auth.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.auth.dto.RegisterRequest;
import com.opsdesk.auth.service.CaptchaService;
import com.opsdesk.auth.service.TokenService;
import com.opsdesk.auth.service.SmsVerificationService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.department.entity.Department;
import com.opsdesk.department.mapper.DepartmentMapper;
import com.opsdesk.role.mapper.RoleMapper;
import com.opsdesk.user.mapper.SysUserMapper;
import com.opsdesk.user.mapper.UserRoleMapper;
import com.opsdesk.user.service.UserContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 认证业务服务测试。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserContextService userContextService;

    @Mock
    private CaptchaService captchaService;

    @Mock
    private TokenService tokenService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private SmsVerificationService smsVerificationService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                sysUserMapper,
                userRoleMapper,
                departmentMapper,
                roleMapper,
                userContextService,
                captchaService,
                tokenService,
                new BCryptPasswordEncoder(),
                new SnowflakeIdGenerator(),
                auditLogService,
                smsVerificationService
        );
    }

    @Test
    void registerShouldRejectDuplicatedPhone() {
        RegisterRequest request = new RegisterRequest();
        request.setPhone("13900000000");
        request.setDepartmentId("1");
        request.setPassword("password123");
        request.setSmsCode("123456");

        Department department = new Department();
        department.setId(1L);
        department.setName("IT 部");
        department.setEnabled(1);
        when(departmentMapper.findEnabledById(1L)).thenReturn(department);
        when(sysUserMapper.countByPhone("13900000000")).thenReturn(1);

        assertThatThrownBy(() -> authService.register(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);
    }
}
