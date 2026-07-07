package com.opsdesk.dashboard.dto;

/**
 * 工作台摘要请求。
 *
 * <p>当前仅支持可选团队筛选；权限范围仍以后端当前登录用户和工单资源范围为准。</p>
 */
public class WorkbenchSummaryRequest {

    private String teamId;

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }
}
