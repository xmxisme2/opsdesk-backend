package com.opsdesk.department.mapper;

import com.opsdesk.department.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 部门数据访问 Mapper。
 *
 * <p>注册和用户资料返回时读取部门基础信息。</p>
 */
@Mapper
public interface DepartmentMapper {

    @Select("""
            SELECT *
            FROM sys_department
            WHERE id = #{id}
              AND enabled = 1
              AND deleted = 0
            LIMIT 1
            """)
    Department findEnabledById(@Param("id") Long id);
}
