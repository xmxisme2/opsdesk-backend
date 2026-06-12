package com.opsdesk.team.converter;

import com.opsdesk.team.entity.Team;
import com.opsdesk.team.entity.TeamMember;
import com.opsdesk.team.vo.TeamMemberVO;
import com.opsdesk.team.vo.TeamVO;
import com.opsdesk.user.entity.SysUser;
import com.opsdesk.user.vo.UserVO;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 团队对象转换器。
 *
 * <p>集中处理团队、团队成员到接口 VO 的字段转换，保持 ID 字符串化和时间格式一致。</p>
 */
@Component
public class TeamConverter {

    /** 团队时间字段输出格式：与后台管理其他列表保持一致。 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TeamVO toVO(Team team,
                       List<Long> departmentIds,
                       List<Long> leaderIds,
                       long memberCount) {
        return new TeamVO(
                String.valueOf(team.getId()),
                team.getName(),
                team.getDescription(),
                departmentIds.stream().map(String::valueOf).toList(),
                leaderIds.stream().map(String::valueOf).toList(),
                memberCount,
                team.getProcessingScope(),
                team.getEnabled() != null && team.getEnabled() == 1,
                team.getCreateTime() == null ? null : DATE_TIME_FORMATTER.format(team.getCreateTime()),
                team.getUpdateTime() == null ? null : DATE_TIME_FORMATTER.format(team.getUpdateTime())
        );
    }

    public TeamMemberVO toMemberVO(TeamMember member, SysUser user, String departmentName) {
        UserVO userVO = new UserVO(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getGender(),
                user.getAvatarCode(),
                user.getAvatarUrl(),
                user.getDepartmentId() == null ? null : String.valueOf(user.getDepartmentId()),
                departmentName,
                List.of(),
                List.of(),
                user.getStatus(),
                user.getCreateTime() == null ? null : DATE_TIME_FORMATTER.format(user.getCreateTime()),
                user.getUpdateTime() == null ? null : DATE_TIME_FORMATTER.format(user.getUpdateTime())
        );
        return new TeamMemberVO(
                userVO,
                member.getLeaderFlag() != null && member.getLeaderFlag() == 1,
                member.getCreateTime() == null ? null : DATE_TIME_FORMATTER.format(member.getCreateTime())
        );
    }
}
