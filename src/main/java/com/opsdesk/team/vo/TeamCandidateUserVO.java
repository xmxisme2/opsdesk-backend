package com.opsdesk.team.vo;

/** 团队候选成员视图，仅返回成员选择所需的非敏感字段。 */
public record TeamCandidateUserVO(String id, String username, String nickname, String phone, String departmentName) {
}
