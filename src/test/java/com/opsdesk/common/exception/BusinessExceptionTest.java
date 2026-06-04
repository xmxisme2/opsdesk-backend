package com.opsdesk.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 业务异常测试。
 */
class BusinessExceptionTest {

    @Test
    void businessExceptionShouldKeepErrorCodeAndMessage() {
        BusinessException exception = new BusinessException(ErrorCode.STATE_CONFLICT, "工单状态不允许当前操作");

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);
        assertThat(exception.getMessage()).isEqualTo("工单状态不允许当前操作");
    }
}

