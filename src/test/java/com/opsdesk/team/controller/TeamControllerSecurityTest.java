package com.opsdesk.team.controller;

import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.team.dto.TeamMemberSearchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 团队控制器权限注解回归测试。
 *
 * <p>团队负责人需要进入成员查询接口选择本团队处理人，最终团队归属仍由 Service 按负责人关系校验。</p>
 */
class TeamControllerSecurityTest {

    @Test
    void managerShouldReachMemberSearchScopeChecks() throws NoSuchMethodException {
        Method method = TeamController.class.getDeclaredMethod(
                "searchMembers", String.class, TeamMemberSearchRequest.class, CurrentUser.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('ADMIN', 'MANAGER')");
    }
}
