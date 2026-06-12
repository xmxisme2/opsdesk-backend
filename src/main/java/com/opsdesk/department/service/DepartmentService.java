package com.opsdesk.department.service;

import com.opsdesk.department.dto.DepartmentCreateRequest;
import com.opsdesk.department.dto.DepartmentTreeRequest;
import com.opsdesk.department.dto.DepartmentUpdateRequest;
import com.opsdesk.department.vo.DepartmentVO;

import java.util.List;

/**
 * 部门管理服务。
 *
 * <p>负责部门树查询、部门 CRUD、父子关系校验和后台组织管理审计。</p>
 */
public interface DepartmentService {

    List<DepartmentVO> tree(DepartmentTreeRequest request);

    DepartmentVO create(DepartmentCreateRequest request, Long operatorId, String requestIp, String userAgent);

    DepartmentVO detail(String id);

    DepartmentVO update(String id, DepartmentUpdateRequest request, Long operatorId, String requestIp, String userAgent);

    void delete(String id, Long operatorId, String requestIp, String userAgent);
}
