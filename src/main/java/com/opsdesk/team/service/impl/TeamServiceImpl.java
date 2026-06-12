package com.opsdesk.team.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.util.IdParser;
import com.opsdesk.department.entity.Department;
import com.opsdesk.department.mapper.DepartmentMapper;
import com.opsdesk.team.converter.TeamConverter;
import com.opsdesk.team.dto.TeamCreateRequest;
import com.opsdesk.team.dto.TeamLeaderUpdateRequest;
import com.opsdesk.team.dto.TeamMemberSearchRequest;
import com.opsdesk.team.dto.TeamMemberUpdateRequest;
import com.opsdesk.team.dto.TeamSearchRequest;
import com.opsdesk.team.dto.TeamUpdateRequest;
import com.opsdesk.team.entity.Team;
import com.opsdesk.team.entity.TeamMember;
import com.opsdesk.team.mapper.TeamMapper;
import com.opsdesk.team.mapper.TeamMemberMapper;
import com.opsdesk.team.service.TeamService;
import com.opsdesk.team.vo.TeamMemberVO;
import com.opsdesk.team.vo.TeamVO;
import com.opsdesk.user.entity.SysUser;
import com.opsdesk.user.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 团队管理服务实现。
 *
 * <p>集中处理团队名称唯一、成员整体替换、负责人保留、团队逻辑删除和审计日志。</p>
 */
@Service
public class TeamServiceImpl implements TeamService {

    /** 团队审计业务类型：团队创建、编辑、删除和成员变更统一归入 TEAM。 */
    private static final String BIZ_TYPE_TEAM = "TEAM";

    /** 团队成员普通角色：非负责人团队成员使用，外部不允许直接传入。 */
    private static final String MEMBER_ROLE_MEMBER = "MEMBER";

    /** 团队负责人角色：leader=true 的团队成员使用，外部不允许直接传入。 */
    private static final String MEMBER_ROLE_LEADER = "LEADER";

    /** 创建团队审计操作类型：管理员新增团队时写入 audit_log。 */
    private static final String OPERATION_TEAM_CREATE = "TEAM_CREATE";

    /** 编辑团队审计操作类型：管理员保存团队基础信息时写入 audit_log。 */
    private static final String OPERATION_TEAM_UPDATE = "TEAM_UPDATE";

    /** 删除团队审计操作类型：管理员逻辑删除团队时写入 audit_log。 */
    private static final String OPERATION_TEAM_DELETE = "TEAM_DELETE";

    /** 更新团队成员审计操作类型：成员整体替换时写入 audit_log。 */
    private static final String OPERATION_TEAM_MEMBER_UPDATE = "TEAM_MEMBER_UPDATE";

    /** 更新团队负责人审计操作类型：负责人集合替换时写入 audit_log。 */
    private static final String OPERATION_TEAM_LEADER_UPDATE = "TEAM_LEADER_UPDATE";

    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final SysUserMapper sysUserMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final AuditLogService auditLogService;
    private final TeamConverter teamConverter;
    private final DepartmentMapper departmentMapper;

    public TeamServiceImpl(TeamMapper teamMapper,
                           TeamMemberMapper teamMemberMapper,
                           SysUserMapper sysUserMapper,
                           SnowflakeIdGenerator idGenerator,
                           AuditLogService auditLogService) {
        this(teamMapper, teamMemberMapper, sysUserMapper, idGenerator, auditLogService, new TeamConverter(), null);
    }

    @Autowired
    public TeamServiceImpl(TeamMapper teamMapper,
                           TeamMemberMapper teamMemberMapper,
                           SysUserMapper sysUserMapper,
                           SnowflakeIdGenerator idGenerator,
                           AuditLogService auditLogService,
                           TeamConverter teamConverter,
                           DepartmentMapper departmentMapper) {
        this.teamMapper = teamMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.sysUserMapper = sysUserMapper;
        this.idGenerator = idGenerator;
        this.auditLogService = auditLogService;
        this.teamConverter = teamConverter;
        this.departmentMapper = departmentMapper;
    }

    @Override
    public PageResult<TeamVO> search(TeamSearchRequest request) {
        TeamSearchRequest safeRequest = request == null ? new TeamSearchRequest() : request;
        long page = safeRequest.normalizedPage();
        long size = safeRequest.normalizedSize();
        Long departmentId = parseOptionalId(safeRequest.getDepartmentId(), "部门ID");
        Integer enabled = safeRequest.getEnabled() == null ? null : (safeRequest.getEnabled() ? 1 : 0);

        long total = teamMapper.countSearch(safeRequest.normalizedKeyword(), departmentId, enabled);
        if (total == 0) {
            return PageResult.empty(page, size);
        }
        long offset = (page - 1) * size;
        List<TeamVO> records = teamMapper.search(safeRequest.normalizedKeyword(), departmentId, enabled, offset, size)
                .stream()
                .map(this::assembleTeamVO)
                .toList();
        return new PageResult<>(records, page, size, total);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamVO create(TeamCreateRequest request, Long operatorId, String requestIp, String userAgent) {
        List<Long> leaderIds = parseRequiredLeaderIds(request.getLeaderIds());
        List<Long> memberIds = mergeMembersAndLeaders(request.getMemberIds(), leaderIds);
        validateUsers(memberIds);
        String name = normalizeName(request.getName());
        if (teamMapper.countByName(name) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "团队名称已存在");
        }

        Team team = new Team();
        team.setId(idGenerator.nextId());
        team.setName(name);
        team.setDescription(trimToNull(request.getDescription()));
        team.setProcessingScope(trimToNull(request.getProcessingScope()));
        team.setEnabled(Boolean.FALSE.equals(request.getEnabled()) ? 0 : 1);
        team.setCreateBy(operatorId);
        team.setUpdateBy(operatorId);
        teamMapper.insert(team);
        replaceMembers(team.getId(), memberIds, Set.copyOf(leaderIds), operatorId);

        auditLogService.record(operatorId, OPERATION_TEAM_CREATE, BIZ_TYPE_TEAM, team.getId(),
                "创建团队：" + team.getName(), requestIp, userAgent);
        return detail(String.valueOf(team.getId()));
    }

    @Override
    public TeamVO detail(String id) {
        Long teamId = IdParser.parseRequired(id, "团队ID");
        return assembleTeamVO(loadTeam(teamId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamVO update(String id, TeamUpdateRequest request, Long operatorId, String requestIp, String userAgent) {
        Long teamId = IdParser.parseRequired(id, "团队ID");
        Team team = loadTeam(teamId);
        String name = normalizeName(request.getName());
        if (teamMapper.countByNameExcludeId(name, teamId) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "团队名称已存在");
        }

        team.setName(name);
        team.setDescription(trimToNull(request.getDescription()));
        team.setProcessingScope(trimToNull(request.getProcessingScope()));
        team.setEnabled(Boolean.FALSE.equals(request.getEnabled()) ? 0 : 1);
        team.setUpdateBy(operatorId);
        if (teamMapper.update(team) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "团队不存在");
        }
        auditLogService.record(operatorId, OPERATION_TEAM_UPDATE, BIZ_TYPE_TEAM, teamId,
                "编辑团队：" + team.getName(), requestIp, userAgent);
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id, Long operatorId, String requestIp, String userAgent) {
        Long teamId = IdParser.parseRequired(id, "团队ID");
        Team team = loadTeam(teamId);
        if (teamMapper.countOpenTickets(teamId) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "团队存在未关闭工单，不能删除");
        }
        if (teamMapper.logicalDelete(teamId, operatorId) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "团队不存在");
        }
        teamMemberMapper.deactivateAllByTeamId(teamId, operatorId);
        auditLogService.record(operatorId, OPERATION_TEAM_DELETE, BIZ_TYPE_TEAM, teamId,
                "删除团队：" + team.getName(), requestIp, userAgent);
    }

    @Override
    public PageResult<TeamMemberVO> searchMembers(String id, TeamMemberSearchRequest request) {
        Long teamId = IdParser.parseRequired(id, "团队ID");
        loadTeam(teamId);
        TeamMemberSearchRequest safeRequest = request == null ? new TeamMemberSearchRequest() : request;
        long page = safeRequest.normalizedPage();
        long size = safeRequest.normalizedSize();
        long total = teamMemberMapper.countSearchMembers(teamId, safeRequest.normalizedKeyword());
        if (total == 0) {
            return PageResult.empty(page, size);
        }
        long offset = (page - 1) * size;
        List<TeamMemberVO> records = teamMemberMapper.searchMembers(teamId, safeRequest.normalizedKeyword(), offset, size)
                .stream()
                .map(this::assembleMemberVO)
                .toList();
        return new PageResult<>(records, page, size, total);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamVO updateMembers(String id,
                                TeamMemberUpdateRequest request,
                                Long operatorId,
                                String requestIp,
                                String userAgent) {
        Long teamId = IdParser.parseRequired(id, "团队ID");
        loadTeam(teamId);
        List<TeamMemberUpdateRequest.MemberItem> members = request == null || request.getMembers() == null
                ? List.of()
                : request.getMembers();
        if (members.stream().noneMatch(item -> Boolean.TRUE.equals(item.getLeader()))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "团队至少需要一名负责人");
        }

        List<Long> memberIds = new ArrayList<>();
        Set<Long> leaderIds = new LinkedHashSet<>();
        for (TeamMemberUpdateRequest.MemberItem item : members) {
            Long userId = IdParser.parseRequired(item.getUserId(), "成员用户ID");
            memberIds.add(userId);
            if (Boolean.TRUE.equals(item.getLeader())) {
                leaderIds.add(userId);
            }
        }
        memberIds = memberIds.stream().distinct().toList();
        validateUsers(memberIds);
        replaceMembers(teamId, memberIds, leaderIds, operatorId);
        auditLogService.record(operatorId, OPERATION_TEAM_MEMBER_UPDATE, BIZ_TYPE_TEAM, teamId,
                "更新团队成员", requestIp, userAgent);
        return detail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamVO updateLeaders(String id,
                                TeamLeaderUpdateRequest request,
                                Long operatorId,
                                String requestIp,
                                String userAgent) {
        Long teamId = IdParser.parseRequired(id, "团队ID");
        loadTeam(teamId);
        List<Long> leaderIds = parseRequiredLeaderIds(request == null ? null : request.getLeaderIds());
        List<Long> activeMemberIds = teamMemberMapper.findActiveUserIdsByTeamId(teamId);
        if (!activeMemberIds.containsAll(leaderIds)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "负责人必须是团队成员");
        }
        teamMemberMapper.resetLeaders(teamId, operatorId);
        teamMemberMapper.markLeaders(teamId, leaderIds, operatorId);
        auditLogService.record(operatorId, OPERATION_TEAM_LEADER_UPDATE, BIZ_TYPE_TEAM, teamId,
                "更新团队负责人", requestIp, userAgent);
        return detail(id);
    }

    private void replaceMembers(Long teamId, List<Long> memberIds, Set<Long> leaderIds, Long operatorId) {
        if (leaderIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "团队至少需要一名负责人");
        }
        teamMemberMapper.deactivateMissing(teamId, memberIds, operatorId);
        for (Long userId : memberIds) {
            TeamMember member = buildMember(teamId, userId, leaderIds.contains(userId), operatorId);
            if (teamMemberMapper.countActive(teamId, userId) > 0) {
                teamMemberMapper.updateMember(member);
                continue;
            }
            if (teamMemberMapper.restoreDeleted(member) == 0) {
                member.setId(idGenerator.nextId());
                teamMemberMapper.insert(member);
            }
        }
    }

    private TeamMember buildMember(Long teamId, Long userId, boolean leader, Long operatorId) {
        TeamMember member = new TeamMember();
        member.setTeamId(teamId);
        member.setUserId(userId);
        member.setMemberRole(leader ? MEMBER_ROLE_LEADER : MEMBER_ROLE_MEMBER);
        member.setLeaderFlag(leader ? 1 : 0);
        member.setCreateBy(operatorId);
        member.setUpdateBy(operatorId);
        return member;
    }

    private TeamVO assembleTeamVO(Team team) {
        return teamConverter.toVO(
                team,
                teamMemberMapper.findDepartmentIdsByTeamId(team.getId()),
                teamMemberMapper.findLeaderIdsByTeamId(team.getId()),
                teamMemberMapper.countMembers(team.getId())
        );
    }

    private TeamMemberVO assembleMemberVO(TeamMember member) {
        SysUser user = sysUserMapper.findById(member.getUserId());
        String departmentName = null;
        if (departmentMapper != null && user != null && user.getDepartmentId() != null) {
            Department department = departmentMapper.findById(user.getDepartmentId());
            departmentName = department == null ? null : department.getName();
        }
        return teamConverter.toMemberVO(member, user, departmentName);
    }

    private Team loadTeam(Long teamId) {
        Team team = teamMapper.findById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "团队不存在");
        }
        return team;
    }

    private List<Long> parseRequiredLeaderIds(List<String> leaderIdValues) {
        List<Long> leaderIds = IdParser.parseDistinctList(leaderIdValues, "负责人ID");
        if (leaderIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "团队至少需要一名负责人");
        }
        return leaderIds;
    }

    private List<Long> mergeMembersAndLeaders(List<String> memberIdValues, List<Long> leaderIds) {
        LinkedHashSet<Long> memberIds = new LinkedHashSet<>(IdParser.parseDistinctList(memberIdValues, "成员用户ID"));
        memberIds.addAll(leaderIds);
        return new ArrayList<>(memberIds);
    }

    private void validateUsers(List<Long> userIds) {
        for (Long userId : userIds) {
            if (sysUserMapper.findById(userId) == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "成员用户不存在");
            }
        }
    }

    private Long parseOptionalId(String value, String fieldName) {
        return StringUtils.hasText(value) ? IdParser.parseRequired(value, fieldName) : null;
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
