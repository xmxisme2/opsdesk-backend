package com.opsdesk.common.response;

import com.opsdesk.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一响应结构测试。
 */
class ApiResponseTest {

    @Test
    void successShouldUseUnifiedSuccessCode() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("success");
        assertThat(response.getData()).isEqualTo("ok");
    }

    @Test
    void errorShouldUseContractErrorCode() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.FORBIDDEN);

        assertThat(response.getCode()).isEqualTo(403001);
        assertThat(response.getMessage()).isEqualTo("无权限");
        assertThat(response.getData()).isNull();
    }
}

