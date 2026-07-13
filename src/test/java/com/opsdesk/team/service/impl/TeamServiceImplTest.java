package com.opsdesk.team.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.team.dto.TeamCreateRequest;
import com.opsdesk.team.dto.TeamLeaderUpdateRequest;
import com.opsdesk.team.dto.TeamMemberUpdateRequest;
import com.opsdesk.team.entity.Team;
import com.opsdesk.team.mapper.TeamMapper;
import com.opsdesk.team.mapper.TeamMemberMapper;
import com.opsdesk.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 团队管理服务单元测试。
 *
 * <p>覆盖团队负责人和成员关系的核心规则，保证团队始终至少保留一名负责人。</p>
 */
@ExtendWith(MockitoExtension.class)
class TeamServiceImplTest {

    @Mock
    private TeamMapper teamMapper;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private AuditLogService auditLogService;

    private TeamServiceImpl teamService;

    @BeforeEach
    void setUp() {
        teamService = new TeamServiceImpl(
                teamMapper,
                teamMemberMapper,
                sysUserMapper,
                new SnowflakeIdGenerator(),
                auditLogService
        );
    }

    @Test
    void createShouldRequireAtLeastOneLeader() {
        TeamCreateRequest request = new TeamCreateRequest();
        request.setName("应用支持组");
        request.setMemberIds(List.of("2"));
        request.setLeaderIds(List.of());

        assertThatThrownBy(() -> teamService.create(request, 1L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARAM_ERROR);

        verify(teamMapper, never()).insert(any());
    }

    @Test
    void updateMembersShouldRequireLeader() {
        when(teamMapper.findById(1L)).thenReturn(existingTeam());
        TeamMemberUpdateRequest request = new TeamMemberUpdateRequest();
        TeamMemberUpdateRequest.MemberItem item = new TeamMemberUpdateRequest.MemberItem();
        item.setUserId("2");
        item.setLeader(false);
        request.setMembers(List.of(item));

        assertThatThrownBy(() -> teamService.updateMembers("1", request, admin(), "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARAM_ERROR);

        verify(teamMemberMapper, never()).deactivateMissing(any(), any(), any());
    }

    @Test
    void managerShouldNotChangeLeaderSetWhenUpdatingMembers() {
        when(teamMapper.findById(1L)).thenReturn(existingTeam());
        when(teamMemberMapper.countLeader(1L, 2L)).thenReturn(1);
        when(teamMemberMapper.findLeaderIdsByTeamId(1L)).thenReturn(List.of(2L));
        TeamMemberUpdateRequest request = new TeamMemberUpdateRequest();
        TeamMemberUpdateRequest.MemberItem item = new TeamMemberUpdateRequest.MemberItem();
        item.setUserId("3");
        item.setLeader(true);
        request.setMembers(List.of(item));

        assertThatThrownBy(() -> teamService.updateMembers("1", request, manager(), "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void updateLeadersShouldRequireLeaderInTeamMembers() {
        when(teamMapper.findById(1L)).thenReturn(existingTeam());
        when(teamMemberMapper.findActiveUserIdsByTeamId(1L)).thenReturn(List.of(2L));

        TeamLeaderUpdateRequest request = new TeamLeaderUpdateRequest();
        request.setLeaderIds(List.of("3"));

        assertThatThrownBy(() -> teamService.updateLeaders("1", request, 1L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARAM_ERROR);

        verify(teamMemberMapper, never()).resetLeaders(any(), any());
    }

    @Test
    void deleteShouldRejectTeamWithOpenTickets() {
        when(teamMapper.findById(1L)).thenReturn(existingTeam());
        when(teamMapper.countOpenTickets(1L)).thenReturn(1);

        assertThatThrownBy(() -> teamService.delete("1", 1L, "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);

        verify(teamMapper, never()).logicalDelete(any(), any());
    }

    private Team existingTeam() {
        Team team = new Team();
        team.setId(1L);
        team.setName("基础设施支持组");
        team.setEnabled(1);
        return team;
    }

    private CurrentUser admin() {
        return new CurrentUser(1L, "13800000000", "admin", List.of("ADMIN"), List.of());
    }

    private CurrentUser manager() {
        return new CurrentUser(2L, "13800000001", "manager", List.of("MANAGER"), List.of());
    }
}
