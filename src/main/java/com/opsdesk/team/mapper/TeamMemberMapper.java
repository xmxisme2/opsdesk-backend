package com.opsdesk.team.mapper;

import com.opsdesk.team.entity.TeamMember;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 团队成员数据访问 Mapper。
 *
 * <p>只负责 team_member 关系表读写，负责人保留规则由服务层校验。</p>
 */
@Mapper
public interface TeamMemberMapper {

    @Select("""
            SELECT COUNT(1)
            FROM team_member
            WHERE team_id = #{teamId}
              AND deleted = 0
            """)
    long countMembers(@Param("teamId") Long teamId);

    @Select("""
            SELECT user_id
            FROM team_member
            WHERE team_id = #{teamId}
              AND leader_flag = 1
              AND deleted = 0
            ORDER BY user_id
            """)
    List<Long> findLeaderIdsByTeamId(@Param("teamId") Long teamId);

    @Select("""
            SELECT tm.user_id
            FROM team_member tm
            WHERE tm.team_id = #{teamId}
              AND tm.deleted = 0
            ORDER BY tm.user_id
            """)
    List<Long> findActiveUserIdsByTeamId(@Param("teamId") Long teamId);

    @Select("""
            SELECT DISTINCT u.department_id
            FROM team_member tm
            INNER JOIN sys_user u ON u.id = tm.user_id
            WHERE tm.team_id = #{teamId}
              AND tm.deleted = 0
              AND u.deleted = 0
              AND u.department_id IS NOT NULL
            ORDER BY u.department_id
            """)
    List<Long> findDepartmentIdsByTeamId(@Param("teamId") Long teamId);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM team_member tm
            INNER JOIN sys_user u ON u.id = tm.user_id AND u.deleted = 0
            WHERE tm.team_id = #{teamId}
              AND tm.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (u.phone LIKE CONCAT('%', #{keyword}, '%')
                   OR u.username LIKE CONCAT('%', #{keyword}, '%')
                   OR u.nickname LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            </script>
            """)
    long countSearchMembers(@Param("teamId") Long teamId,
                            @Param("keyword") String keyword);

    @Select("""
            <script>
            SELECT tm.*
            FROM team_member tm
            INNER JOIN sys_user u ON u.id = tm.user_id AND u.deleted = 0
            WHERE tm.team_id = #{teamId}
              AND tm.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (u.phone LIKE CONCAT('%', #{keyword}, '%')
                   OR u.username LIKE CONCAT('%', #{keyword}, '%')
                   OR u.nickname LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY tm.leader_flag DESC, tm.create_time ASC, tm.user_id ASC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<TeamMember> searchMembers(@Param("teamId") Long teamId,
                                   @Param("keyword") String keyword,
                                   @Param("offset") long offset,
                                   @Param("size") long size);

    @Select("""
            SELECT COUNT(1)
            FROM team_member
            WHERE team_id = #{teamId}
              AND user_id = #{userId}
              AND deleted = 0
            """)
    int countActive(@Param("teamId") Long teamId,
                    @Param("userId") Long userId);

    @Insert("""
            INSERT INTO team_member (
              id, team_id, user_id, member_role, leader_flag, create_by, update_by, deleted
            )
            VALUES (
              #{id}, #{teamId}, #{userId}, #{memberRole}, #{leaderFlag}, #{createBy}, #{updateBy}, 0
            )
            """)
    int insert(TeamMember member);

    @Update("""
            UPDATE team_member
            SET deleted = 0,
                member_role = #{memberRole},
                leader_flag = #{leaderFlag},
                update_by = #{updateBy},
                update_time = CURRENT_TIMESTAMP
            WHERE team_id = #{teamId}
              AND user_id = #{userId}
              AND deleted = 1
            """)
    int restoreDeleted(TeamMember member);

    @Update("""
            UPDATE team_member
            SET member_role = #{memberRole},
                leader_flag = #{leaderFlag},
                update_by = #{updateBy},
                update_time = CURRENT_TIMESTAMP
            WHERE team_id = #{teamId}
              AND user_id = #{userId}
              AND deleted = 0
            """)
    int updateMember(TeamMember member);

    @Update("""
            <script>
            UPDATE team_member
            SET deleted = 1,
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE team_id = #{teamId}
              AND deleted = 0
            <if test="userIds != null and userIds.size() > 0">
              AND user_id NOT IN
              <foreach collection="userIds" item="userId" open="(" separator="," close=")">
                #{userId}
              </foreach>
            </if>
            </script>
            """)
    int deactivateMissing(@Param("teamId") Long teamId,
                          @Param("userIds") List<Long> userIds,
                          @Param("operatorId") Long operatorId);

    @Update("""
            UPDATE team_member
            SET deleted = 1,
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE team_id = #{teamId}
              AND deleted = 0
            """)
    int deactivateAllByTeamId(@Param("teamId") Long teamId,
                              @Param("operatorId") Long operatorId);

    @Update("""
            UPDATE team_member
            SET leader_flag = 0,
                member_role = 'MEMBER',
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE team_id = #{teamId}
              AND deleted = 0
            """)
    int resetLeaders(@Param("teamId") Long teamId,
                     @Param("operatorId") Long operatorId);

    @Update("""
            <script>
            UPDATE team_member
            SET leader_flag = 1,
                member_role = 'LEADER',
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE team_id = #{teamId}
              AND deleted = 0
              AND user_id IN
              <foreach collection="leaderIds" item="leaderId" open="(" separator="," close=")">
                #{leaderId}
              </foreach>
            </script>
            """)
    int markLeaders(@Param("teamId") Long teamId,
                    @Param("leaderIds") List<Long> leaderIds,
                    @Param("operatorId") Long operatorId);
}
