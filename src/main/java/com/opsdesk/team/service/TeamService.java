package com.opsdesk.team.service;

import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.team.dto.TeamCreateRequest;
import com.opsdesk.team.dto.TeamLeaderUpdateRequest;
import com.opsdesk.team.dto.TeamMemberSearchRequest;
import com.opsdesk.team.dto.TeamMemberUpdateRequest;
import com.opsdesk.team.dto.TeamSearchRequest;
import com.opsdesk.team.dto.TeamUpdateRequest;
import com.opsdesk.team.vo.TeamMemberVO;
import com.opsdesk.team.vo.TeamVO;

/**
 * 团队管理服务。
 *
 * <p>负责处理团队基础信息、成员列表、成员替换和负责人规则。</p>
 */
public interface TeamService {

    PageResult<TeamVO> search(TeamSearchRequest request);

    TeamVO create(TeamCreateRequest request, Long operatorId, String requestIp, String userAgent);

    TeamVO detail(String id);

    TeamVO update(String id, TeamUpdateRequest request, Long operatorId, String requestIp, String userAgent);

    void delete(String id, Long operatorId, String requestIp, String userAgent);

    PageResult<TeamMemberVO> searchMembers(String id, TeamMemberSearchRequest request, CurrentUser currentUser);

    TeamVO updateMembers(String id, TeamMemberUpdateRequest request, Long operatorId, String requestIp, String userAgent);

    TeamVO updateLeaders(String id, TeamLeaderUpdateRequest request, Long operatorId, String requestIp, String userAgent);
}
