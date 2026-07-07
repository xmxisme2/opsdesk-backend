package com.opsdesk.department.mapper;

import com.opsdesk.department.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门数据访问 Mapper。
 *
 * <p>注册和用户资料返回时读取部门基础信息。</p>
 */
@Mapper
public interface DepartmentMapper {

    Department findById(@Param("id") Long id);

    Department findEnabledById(@Param("id") Long id);

    List<Department> searchTree(@Param("keyword") String keyword,
                                @Param("enabled") Integer enabled);

    int countByParentAndName(@Param("parentId") Long parentId,
                             @Param("name") String name,
                             @Param("excludeId") Long excludeId);

    int countChildren(@Param("parentId") Long parentId);

    List<Long> findDescendantIds(@Param("departmentId") Long departmentId);

    int insert(Department department);

    int update(Department department);

    int logicalDelete(@Param("id") Long id,
                      @Param("operatorId") Long operatorId);
}
