package com.opsdesk.ticket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工单控制器权限注解回归测试。
 *
 * <p>普通用户被明确指派为处理人时也需要进入后端动作接口，真实资源范围由 Service 和状态机继续校验。</p>
 */
class TicketControllerSecurityTest {

    @Test
    void assignedUserActionsShouldReachServiceScopeChecks() throws NoSuchMethodException {
        assertPreAuthorize("accept", "isAuthenticated()");
        assertPreAuthorize("transfer", "isAuthenticated()");
        assertPreAuthorize("complete", "isAuthenticated()");
        assertPreAuthorize("reject", "isAuthenticated()");
    }

    private void assertPreAuthorize(String methodName, String expected) throws NoSuchMethodException {
        Method method = findMethod(methodName);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expected);
    }

    private Method findMethod(String methodName) {
        for (Method method : TicketController.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new IllegalArgumentException("未找到工单控制器方法：" + methodName);
    }
}
