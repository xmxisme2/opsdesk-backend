package com.opsdesk.permission.service.impl;

import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.permission.converter.PermissionConverter;
import com.opsdesk.permission.dto.PermissionTreeRequest;
import com.opsdesk.permission.entity.Permission;
import com.opsdesk.permission.mapper.PermissionMapper;
import com.opsdesk.permission.service.PermissionService;
import com.opsdesk.permission.vo.PermissionVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限服务实现。
 *
 * <p>按父子关系组装权限树，根节点包含父节点为空或父节点未出现在本次筛选结果中的权限。</p>
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    /** 权限类型白名单：权限树仅允许菜单、按钮和接口权限三类节点。 */
    private static final List<String> SUPPORTED_TYPES = List.of("MENU", "BUTTON", "API");

    private final PermissionMapper permissionMapper;
    private final PermissionConverter permissionConverter;

    public PermissionServiceImpl(PermissionMapper permissionMapper,
                                 PermissionConverter permissionConverter) {
        this.permissionMapper = permissionMapper;
        this.permissionConverter = permissionConverter;
    }

    @Override
    public List<PermissionVO> tree(PermissionTreeRequest request) {
        PermissionTreeRequest safeRequest = request == null ? new PermissionTreeRequest() : request;
        String type = normalizeType(safeRequest.getType());
        Integer enabled = safeRequest.getEnabled() == null ? null : (safeRequest.getEnabled() ? 1 : 0);
        List<Permission> permissions = permissionMapper.findAll(type, enabled);
        return buildTree(permissions);
    }

    private String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        String normalizedType = type.trim().toUpperCase();
        if (!SUPPORTED_TYPES.contains(normalizedType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "权限类型不正确");
        }
        return normalizedType;
    }

    private List<PermissionVO> buildTree(List<Permission> permissions) {
        Map<Long, PermissionVO> nodeMap = new LinkedHashMap<>();
        Map<Long, Long> parentMap = new LinkedHashMap<>();
        for (Permission permission : permissions) {
            nodeMap.put(permission.getId(), permissionConverter.toVO(permission));
            parentMap.put(permission.getId(), permission.getParentId());
        }

        List<PermissionVO> roots = new ArrayList<>();
        for (Map.Entry<Long, PermissionVO> entry : nodeMap.entrySet()) {
            Long parentId = parentMap.get(entry.getKey());
            PermissionVO node = entry.getValue();
            if (parentId == null || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodeMap.get(parentId).getChildren().add(node);
            }
        }
        sortTree(roots);
        return roots;
    }

    private void sortTree(List<PermissionVO> nodes) {
        nodes.sort(Comparator
                .comparing((PermissionVO node) -> node.getSort() == null ? 0 : node.getSort())
                .thenComparing(PermissionVO::getId));
        nodes.forEach(node -> sortTree(node.getChildren()));
    }
}
