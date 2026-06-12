package com.opsdesk.department.converter;

import com.opsdesk.department.entity.Department;
import com.opsdesk.department.vo.DepartmentVO;
import com.opsdesk.user.entity.SysUser;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 部门对象转换器。
 *
 * <p>集中处理部门 Entity 到 VO 的字段转换，包括 Long ID 字符串化、负责人展示名和树形 children。</p>
 */
@Component
public class DepartmentConverter {

    /** 部门时间字段输出格式：与用户管理接口保持一致，方便前端后台页面展示。 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DepartmentVO toVO(Department department,
                             SysUser leader,
                             long memberCount,
                             List<DepartmentVO> children) {
        return new DepartmentVO(
                String.valueOf(department.getId()),
                department.getParentId() == null ? null : String.valueOf(department.getParentId()),
                department.getName(),
                department.getLeaderId() == null ? null : String.valueOf(department.getLeaderId()),
                resolveLeaderName(leader),
                department.getSort(),
                department.getEnabled() != null && department.getEnabled() == 1,
                memberCount,
                department.getCreateTime() == null ? null : DATE_TIME_FORMATTER.format(department.getCreateTime()),
                department.getUpdateTime() == null ? null : DATE_TIME_FORMATTER.format(department.getUpdateTime()),
                children
        );
    }

    private String resolveLeaderName(SysUser leader) {
        if (leader == null) {
            return null;
        }
        return leader.getNickname() == null || leader.getNickname().isBlank()
                ? leader.getUsername()
                : leader.getNickname();
    }
}
