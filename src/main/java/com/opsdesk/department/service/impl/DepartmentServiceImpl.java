package com.opsdesk.department.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.util.IdParser;
import com.opsdesk.department.converter.DepartmentConverter;
import com.opsdesk.department.dto.DepartmentCreateRequest;
import com.opsdesk.department.dto.DepartmentTreeRequest;
import com.opsdesk.department.dto.DepartmentUpdateRequest;
import com.opsdesk.department.entity.Department;
import com.opsdesk.department.mapper.DepartmentMapper;
import com.opsdesk.department.service.DepartmentService;
import com.opsdesk.department.vo.DepartmentVO;
import com.opsdesk.user.entity.SysUser;
import com.opsdesk.user.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门管理服务实现。
 *
 * <p>集中处理部门树组装、同级名称唯一、父子循环防护、删除保护和审计日志。</p>
 */
@Service
public class DepartmentServiceImpl implements DepartmentService {

    /** 部门审计业务类型：部门创建、编辑和删除统一归入 DEPARTMENT，不允许外部传入。 */
    private static final String BIZ_TYPE_DEPARTMENT = "DEPARTMENT";

    /** 创建部门审计操作类型：管理员新增部门时写入 audit_log，不允许外部传入。 */
    private static final String OPERATION_DEPARTMENT_CREATE = "DEPARTMENT_CREATE";

    /** 编辑部门审计操作类型：管理员保存部门时写入 audit_log，不允许外部传入。 */
    private static final String OPERATION_DEPARTMENT_UPDATE = "DEPARTMENT_UPDATE";

    /** 删除部门审计操作类型：管理员逻辑删除部门时写入 audit_log，不允许外部传入。 */
    private static final String OPERATION_DEPARTMENT_DELETE = "DEPARTMENT_DELETE";

    private final DepartmentMapper departmentMapper;
    private final SysUserMapper sysUserMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final AuditLogService auditLogService;
    private final DepartmentConverter departmentConverter;

    public DepartmentServiceImpl(DepartmentMapper departmentMapper,
                                 SysUserMapper sysUserMapper,
                                 SnowflakeIdGenerator idGenerator,
                                 AuditLogService auditLogService) {
        this(departmentMapper, sysUserMapper, idGenerator, auditLogService, new DepartmentConverter());
    }

    @Autowired
    public DepartmentServiceImpl(DepartmentMapper departmentMapper,
                                 SysUserMapper sysUserMapper,
                                 SnowflakeIdGenerator idGenerator,
                                 AuditLogService auditLogService,
                                 DepartmentConverter departmentConverter) {
        this.departmentMapper = departmentMapper;
        this.sysUserMapper = sysUserMapper;
        this.idGenerator = idGenerator;
        this.auditLogService = auditLogService;
        this.departmentConverter = departmentConverter;
    }

    @Override
    public List<DepartmentVO> tree(DepartmentTreeRequest request) {
        DepartmentTreeRequest safeRequest = request == null ? new DepartmentTreeRequest() : request;
        Integer enabled = safeRequest.getEnabled() == null ? null : (safeRequest.getEnabled() ? 1 : 0);
        List<Department> departments = departmentMapper.searchTree(safeRequest.normalizedKeyword(), enabled);
        return buildTree(departments);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentVO create(DepartmentCreateRequest request, Long operatorId, String requestIp, String userAgent) {
        Long parentId = parseOptionalId(request.getParentId(), "父级部门ID");
        if (parentId != null) {
            loadDepartment(parentId, "父级部门不存在");
        }
        Long leaderId = parseAndValidateOptionalUser(request.getLeaderId(), "负责人ID");
        String name = normalizeName(request.getName());
        if (departmentMapper.countByParentAndName(parentId, name, null) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "同级部门名称已存在");
        }

        Department department = new Department();
        department.setId(idGenerator.nextId());
        department.setParentId(parentId);
        department.setName(name);
        department.setLeaderId(leaderId);
        department.setSort(request.getSort() == null ? 0 : request.getSort());
        department.setEnabled(Boolean.FALSE.equals(request.getEnabled()) ? 0 : 1);
        department.setCreateBy(operatorId);
        department.setUpdateBy(operatorId);
        departmentMapper.insert(department);

        auditLogService.record(operatorId, OPERATION_DEPARTMENT_CREATE, BIZ_TYPE_DEPARTMENT, department.getId(),
                "创建部门：" + department.getName(), requestIp, userAgent);
        return detail(String.valueOf(department.getId()));
    }

    @Override
    public DepartmentVO detail(String id) {
        Long departmentId = IdParser.parseRequired(id, "部门ID");
        return assembleDepartmentVO(loadDepartment(departmentId, "部门不存在"), List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentVO update(String id,
                               DepartmentUpdateRequest request,
                               Long operatorId,
                               String requestIp,
                               String userAgent) {
        Long departmentId = IdParser.parseRequired(id, "部门ID");
        Department department = loadDepartment(departmentId, "部门不存在");
        Long parentId = parseOptionalId(request.getParentId(), "父级部门ID");
        validateParent(departmentId, parentId);
        Long leaderId = parseAndValidateOptionalUser(request.getLeaderId(), "负责人ID");
        String name = normalizeName(request.getName());
        if (departmentMapper.countByParentAndName(parentId, name, departmentId) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "同级部门名称已存在");
        }

        department.setParentId(parentId);
        department.setName(name);
        department.setLeaderId(leaderId);
        department.setSort(request.getSort() == null ? 0 : request.getSort());
        department.setEnabled(Boolean.FALSE.equals(request.getEnabled()) ? 0 : 1);
        department.setUpdateBy(operatorId);
        if (departmentMapper.update(department) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "部门不存在");
        }

        auditLogService.record(operatorId, OPERATION_DEPARTMENT_UPDATE, BIZ_TYPE_DEPARTMENT, departmentId,
                "编辑部门：" + department.getName(), requestIp, userAgent);
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id, Long operatorId, String requestIp, String userAgent) {
        Long departmentId = IdParser.parseRequired(id, "部门ID");
        Department department = loadDepartment(departmentId, "部门不存在");
        if (departmentMapper.countChildren(departmentId) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "存在子部门，不能删除");
        }
        if (sysUserMapper.countByDepartmentId(departmentId) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "部门下仍有关联用户，不能删除");
        }
        if (departmentMapper.logicalDelete(departmentId, operatorId) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "部门不存在");
        }
        auditLogService.record(operatorId, OPERATION_DEPARTMENT_DELETE, BIZ_TYPE_DEPARTMENT, departmentId,
                "删除部门：" + department.getName(), requestIp, userAgent);
    }

    private List<DepartmentVO> buildTree(List<Department> departments) {
        Map<Long, List<Department>> childrenByParent = new LinkedHashMap<>();
        for (Department department : departments) {
            childrenByParent.computeIfAbsent(department.getParentId(), ignored -> new ArrayList<>()).add(department);
        }

        List<DepartmentVO> roots = new ArrayList<>();
        for (Department department : departments) {
            if (department.getParentId() == null || departments.stream().noneMatch(item -> item.getId().equals(department.getParentId()))) {
                roots.add(toTreeNode(department, childrenByParent));
            }
        }
        return roots;
    }

    private DepartmentVO toTreeNode(Department department, Map<Long, List<Department>> childrenByParent) {
        List<DepartmentVO> children = childrenByParent.getOrDefault(department.getId(), List.of())
                .stream()
                .map(child -> toTreeNode(child, childrenByParent))
                .toList();
        return assembleDepartmentVO(department, children);
    }

    private DepartmentVO assembleDepartmentVO(Department department, List<DepartmentVO> children) {
        SysUser leader = department.getLeaderId() == null ? null : sysUserMapper.findById(department.getLeaderId());
        return departmentConverter.toVO(department, leader, sysUserMapper.countByDepartmentId(department.getId()), children);
    }

    private Department loadDepartment(Long departmentId, String message) {
        Department department = departmentMapper.findById(departmentId);
        if (department == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, message);
        }
        return department;
    }

    private void validateParent(Long departmentId, Long parentId) {
        if (parentId == null) {
            return;
        }
        if (departmentId.equals(parentId)) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "父级部门不能选择自身");
        }
        if (departmentMapper.findDescendantIds(departmentId).contains(parentId)) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "父级部门不能选择当前部门的子部门");
        }
        loadDepartment(parentId, "父级部门不存在");
    }

    private Long parseAndValidateOptionalUser(String value, String fieldName) {
        Long userId = parseOptionalId(value, fieldName);
        if (userId == null) {
            return null;
        }
        if (sysUserMapper.findById(userId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "负责人不存在");
        }
        return userId;
    }

    private Long parseOptionalId(String value, String fieldName) {
        return StringUtils.hasText(value) ? IdParser.parseRequired(value, fieldName) : null;
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }
}
