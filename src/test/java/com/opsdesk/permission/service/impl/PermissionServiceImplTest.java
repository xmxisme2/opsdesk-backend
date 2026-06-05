package com.opsdesk.permission.service.impl;

import com.opsdesk.permission.converter.PermissionConverter;
import com.opsdesk.permission.entity.Permission;
import com.opsdesk.permission.mapper.PermissionMapper;
import com.opsdesk.permission.vo.PermissionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 权限服务单元测试。
 *
 * <p>验证权限树按 parentId 组装，确保角色授权页能拿到稳定层级结构。</p>
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock
    private PermissionMapper permissionMapper;

    private PermissionServiceImpl permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new PermissionServiceImpl(permissionMapper, new PermissionConverter());
    }

    @Test
    void treeShouldBuildParentChildHierarchy() {
        Permission parent = permission(10L, null, "MENU_SYSTEM", "系统管理", "MENU", 10);
        Permission child = permission(11L, 10L, "MENU_SYSTEM_ROLES", "角色管理", "MENU", 20);
        when(permissionMapper.findAll(null, null)).thenReturn(List.of(parent, child));

        List<PermissionVO> tree = permissionService.tree(null);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getCode()).isEqualTo("MENU_SYSTEM");
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getCode()).isEqualTo("MENU_SYSTEM_ROLES");
    }

    private Permission permission(Long id, Long parentId, String code, String name, String type, Integer sort) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setParentId(parentId);
        permission.setCode(code);
        permission.setName(name);
        permission.setType(type);
        permission.setSort(sort);
        permission.setEnabled(1);
        return permission;
    }
}
