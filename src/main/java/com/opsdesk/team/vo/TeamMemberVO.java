package com.opsdesk.team.vo;

import com.opsdesk.user.vo.UserVO;

/**
 * 团队成员响应对象。
 *
 * <p>成员列表返回用户摘要、负责人标记和加入时间，供组织管理页展示。</p>
 */
public record TeamMemberVO(
        UserVO user,
        Boolean leader,
        String joinedAt
) {
}
