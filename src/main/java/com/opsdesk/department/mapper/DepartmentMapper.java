package com.opsdesk.department.mapper;

import com.opsdesk.department.entity.Department;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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

    @Insert("""
            INSERT INTO sys_department (
              id, parent_id, name, leader_id, sort, enabled, create_by, update_by, deleted
            )
            VALUES (
              #{id}, #{parentId}, #{name}, #{leaderId}, #{sort}, #{enabled}, #{createBy}, #{updateBy}, 0
            )
            """)
    int insert(Department department);

    @Update("""
            UPDATE sys_department
            SET parent_id = #{parentId},
                name = #{name},
                leader_id = #{leaderId},
                sort = #{sort},
                enabled = #{enabled},
                update_by = #{updateBy},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND deleted = 0
            """)
    int update(Department department);

    @Update("""
            UPDATE sys_department
            SET deleted = 1,
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND deleted = 0
            """)
    int logicalDelete(@Param("id") Long id,
                      @Param("operatorId") Long operatorId);
}
