package com.opsdesk.team.mapper;

import com.opsdesk.team.entity.TeamMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 团队成员数据访问 Mapper。
 *
 * <p>只负责 team_member 关系表读写，负责人保留规则由服务层校验。</p>
 */
@Mapper
public interface TeamMemberMapper {

    long countMembers(@Param("teamId") Long teamId);

    List<Long> findLeaderIdsByTeamId(@Param("teamId") Long teamId);

    List<Long> findActiveUserIdsByTeamId(@Param("teamId") Long teamId);

    List<Long> findDepartmentIdsByTeamId(@Param("teamId") Long teamId);

    List<TeamMember> searchMembers(@Param("teamId") Long teamId,
                                   @Param("keyword") String keyword);

    int countActive(@Param("teamId") Long teamId,
                    @Param("userId") Long userId);

    int countLeader(@Param("teamId") Long teamId,
                    @Param("userId") Long userId);

    int insert(TeamMember member);

    int restoreDeleted(TeamMember member);

    int updateMember(TeamMember member);

    int deactivateMissing(@Param("teamId") Long teamId,
                          @Param("userIds") List<Long> userIds,
                          @Param("operatorId") Long operatorId);

    int deactivateAllByTeamId(@Param("teamId") Long teamId,
                              @Param("operatorId") Long operatorId);

    int resetLeaders(@Param("teamId") Long teamId,
                     @Param("operatorId") Long operatorId);

    int markLeaders(@Param("teamId") Long teamId,
                    @Param("leaderIds") List<Long> leaderIds,
                    @Param("operatorId") Long operatorId);
}
