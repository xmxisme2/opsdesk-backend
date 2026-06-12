package com.opsdesk.team.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 团队负责人更新请求。
 *
 * <p>负责人必须已经是团队成员，服务层负责校验并整体替换负责人集合。</p>
 */
@Getter
@Setter
public class TeamLeaderUpdateRequest {

    private List<String> leaderIds;
}
