package com.opsdesk.team.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 团队成员整体更新请求。
 *
 * <p>前端提交完整成员集合，服务层按集合替换团队成员并保证至少一名负责人。</p>
 */
@Getter
@Setter
public class TeamMemberUpdateRequest {

    @Valid
    private List<MemberItem> members;

    /**
     * 团队成员项。
     *
     * <p>userId 使用字符串传输；leader 表示该成员是否为团队负责人。</p>
     */
    @Getter
    @Setter
    public static class MemberItem {

        @NotBlank(message = "成员用户不能为空")
        private String userId;

        private Boolean leader = false;
    }
}
