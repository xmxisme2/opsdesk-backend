package com.opsdesk.department.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.department.dto.DepartmentCreateRequest;
import com.opsdesk.department.dto.DepartmentUpdateRequest;
import com.opsdesk.department.entity.Department;
import com.opsdesk.department.mapper.DepartmentMapper;
import com.opsdesk.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 部门管理服务单元测试。
 *
 * <p>覆盖部门树维护中的同级重名、父级循环和删除保护，避免组织结构被后台操作破坏。</p>
 */
@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private AuditLogService auditLogService;

    private DepartmentServiceImpl departmentService;

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentServiceImpl(
                departmentMapper,
                sysUserMapper,
                new SnowflakeIdGenerator(),
                auditLogService
        );
    }

    @Test
    void createShouldRejectDuplicatedSiblingName() {
        DepartmentCreateRequest request = new DepartmentCreateRequest();
        request.setParentId("1");
        request.setName("IT 部");
        request.setEnabled(true);

        when(departmentMapper.findById(1L)).thenReturn(existingDepartment(1L, null, "OpsDesk 公司"));
        when(departmentMapper.countByParentAndName(1L, "IT 部", null)).thenReturn(1);

        assertThatThrownBy(() -> departmentService.create(request, 1L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);

        verify(departmentMapper, never()).insert(any());
    }

    @Test
    void updateShouldRejectCycleParent() {
        Department department = existingDepartment(2L, 1L, "IT 部");
        when(departmentMapper.findById(2L)).thenReturn(department);
        when(departmentMapper.findDescendantIds(2L)).thenReturn(List.of(3L));

        DepartmentUpdateRequest request = new DepartmentUpdateRequest();
        request.setParentId("3");
        request.setName("IT 支持部");
        request.setEnabled(true);

        assertThatThrownBy(() -> departmentService.update("2", request, 1L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);

        verify(departmentMapper, never()).update(any());
    }

    @Test
    void deleteShouldRejectDepartmentWithUsers() {
        when(departmentMapper.findById(2L)).thenReturn(existingDepartment(2L, 1L, "IT 部"));
        when(departmentMapper.countChildren(2L)).thenReturn(0);
        when(sysUserMapper.countByDepartmentId(2L)).thenReturn(2);

        assertThatThrownBy(() -> departmentService.delete("2", 1L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);

        verify(departmentMapper, never()).logicalDelete(eq(2L), eq(1L));
    }

    private Department existingDepartment(Long id, Long parentId, String name) {
        Department department = new Department();
        department.setId(id);
        department.setParentId(parentId);
        department.setName(name);
        department.setLeaderId(1L);
        department.setSort(10);
        department.setEnabled(1);
        return department;
    }
}
