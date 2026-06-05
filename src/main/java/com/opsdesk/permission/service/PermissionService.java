package com.opsdesk.permission.service;

import com.opsdesk.permission.dto.PermissionTreeRequest;
import com.opsdesk.permission.vo.PermissionVO;

import java.util.List;

/**
 * 权限服务。
 *
 * <p>负责向角色管理页提供菜单、按钮和接口权限树。</p>
 */
public interface PermissionService {

    List<PermissionVO> tree(PermissionTreeRequest request);
}
