package com.opsdesk.department.mapper;

import com.opsdesk.department.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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
              AND deleted = 0
            LIMIT 1
            """)
    Department findById(@Param("id") Long id);

    @Select("""
            SELECT *
            FROM sys_department
            WHERE id = #{id}
              AND enabled = 1
              AND deleted = 0
            LIMIT 1
            """)
    Department findEnabledById(@Param("id") Long id);

    @Select("""
            <script>
            SELECT *
            FROM sys_department
            WHERE deleted = 0
            <if test="keyword != null and keyword != ''">
              AND name LIKE CONCAT('%', #{keyword}, '%')
            </if>
            <if test="enabled != null">
              AND enabled = #{enabled}
            </if>
            ORDER BY sort ASC, id ASC
            </script>
            """)
    List<Department> searchTree(@Param("keyword") String keyword,
                                @Param("enabled") Integer enabled);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM sys_department
            WHERE deleted = 0
              AND name = #{name}
            <choose>
              <when test="parentId == null">
                AND parent_id IS NULL
              </when>
              <otherwise>
                AND parent_id = #{parentId}
              </otherwise>
            </choose>
            <if test="excludeId != null">
              AND id != #{excludeId}
            </if>
            </script>
            """)
    int countByParentAndName(@Param("parentId") Long parentId,
                             @Param("name") String name,
                             @Param("excludeId") Long excludeId);

    @Select("""
            SELECT COUNT(1)
            FROM sys_department
            WHERE parent_id = #{parentId}
              AND deleted = 0
            """)
    int countChildren(@Param("parentId") Long parentId);

    @Select("""
            WITH RECURSIVE department_tree AS (
              SELECT id
              FROM sys_department
              WHERE parent_id = #{departmentId}
                AND deleted = 0
              UNION ALL
              SELECT d.id
              FROM sys_department d
              INNER JOIN department_tree dt ON d.parent_id = dt.id
              WHERE d.deleted = 0
            )
            SELECT id
            FROM department_tree
            """)
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
