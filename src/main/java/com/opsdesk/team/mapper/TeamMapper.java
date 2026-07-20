package com.opsdesk.team.mapper;

import com.opsdesk.team.entity.Team;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    List<Team> searchManagedByLeader(@Param("leaderId") Long leaderId, @Param("keyword") String keyword);

    int insert(Team team);

    int update(Team team);

    int logicalDelete(@Param("id") Long id,
                      @Param("operatorId") Long operatorId);
}
