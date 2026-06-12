package com.opsdesk.team.mapper;

import com.opsdesk.team.entity.Team;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 团队数据访问 Mapper。
 *
 * <p>只负责 team 表读写和团队列表筛选，不包含负责人、成员替换等业务规则。</p>
 */
@Mapper
public interface TeamMapper {

    @Select("""
            SELECT *
            FROM team
            WHERE id = #{id}
              AND deleted = 0
            LIMIT 1
            """)
    Team findById(@Param("id") Long id);

    @Select("""
            SELECT COUNT(1)
            FROM team
            WHERE name = #{name}
              AND deleted = 0
            """)
    int countByName(@Param("name") String name);

    @Select("""
            SELECT COUNT(1)
            FROM team
            WHERE name = #{name}
              AND id != #{excludeId}
              AND deleted = 0
            """)
    int countByNameExcludeId(@Param("name") String name,
                             @Param("excludeId") Long excludeId);

    @Select("""
            SELECT COUNT(1)
            FROM ticket
            WHERE team_id = #{teamId}
              AND deleted = 0
              AND status NOT IN ('CLOSED', 'CANCELLED')
            """)
    int countOpenTickets(@Param("teamId") Long teamId);

    @Select("""
            <script>
            SELECT COUNT(DISTINCT t.id)
            FROM team t
            <if test="departmentId != null">
              INNER JOIN team_member tm ON tm.team_id = t.id AND tm.deleted = 0
              INNER JOIN sys_user u ON u.id = tm.user_id AND u.deleted = 0
            </if>
            WHERE t.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (t.name LIKE CONCAT('%', #{keyword}, '%')
                   OR t.description LIKE CONCAT('%', #{keyword}, '%')
                   OR t.processing_scope LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="departmentId != null">
              AND u.department_id = #{departmentId}
            </if>
            <if test="enabled != null">
              AND t.enabled = #{enabled}
            </if>
            </script>
            """)
    long countSearch(@Param("keyword") String keyword,
                     @Param("departmentId") Long departmentId,
                     @Param("enabled") Integer enabled);

    @Select("""
            <script>
            SELECT DISTINCT t.*
            FROM team t
            <if test="departmentId != null">
              INNER JOIN team_member tm ON tm.team_id = t.id AND tm.deleted = 0
              INNER JOIN sys_user u ON u.id = tm.user_id AND u.deleted = 0
            </if>
            WHERE t.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (t.name LIKE CONCAT('%', #{keyword}, '%')
                   OR t.description LIKE CONCAT('%', #{keyword}, '%')
                   OR t.processing_scope LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="departmentId != null">
              AND u.department_id = #{departmentId}
            </if>
            <if test="enabled != null">
              AND t.enabled = #{enabled}
            </if>
            ORDER BY t.create_time DESC, t.id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<Team> search(@Param("keyword") String keyword,
                      @Param("departmentId") Long departmentId,
                      @Param("enabled") Integer enabled,
                      @Param("offset") long offset,
                      @Param("size") long size);

    @Insert("""
            INSERT INTO team (
              id, name, description, processing_scope, enabled, create_by, update_by, deleted
            )
            VALUES (
              #{id}, #{name}, #{description}, #{processingScope}, #{enabled}, #{createBy}, #{updateBy}, 0
            )
            """)
    int insert(Team team);

    @Update("""
            UPDATE team
            SET name = #{name},
                description = #{description},
                processing_scope = #{processingScope},
                enabled = #{enabled},
                update_by = #{updateBy},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND deleted = 0
            """)
    int update(Team team);

    @Update("""
            UPDATE team
            SET deleted = 1,
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND deleted = 0
            """)
    int logicalDelete(@Param("id") Long id,
                      @Param("operatorId") Long operatorId);
}
