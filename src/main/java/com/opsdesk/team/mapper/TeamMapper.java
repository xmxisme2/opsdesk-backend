package com.opsdesk.team.mapper;

import com.opsdesk.team.entity.Team;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 团队数据访问 Mapper。
 *
 * <p>只负责 team 表读写和团队列表筛选，不包含负责人、成员替换等业务规则。</p>
 */
@Mapper
public interface TeamMapper {

    Team findById(@Param("id") Long id);

    int countByName(@Param("name") String name);

    int countByNameExcludeId(@Param("name") String name,
                             @Param("excludeId") Long excludeId);

    int countOpenTickets(@Param("teamId") Long teamId);

    List<Team> search(@Param("keyword") String keyword,
                      @Param("departmentId") Long departmentId,
                      @Param("enabled") Integer enabled);

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
