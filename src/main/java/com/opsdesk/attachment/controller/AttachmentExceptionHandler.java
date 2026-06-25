package com.opsdesk.attachment.controller;

import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 附件上传异常处理器。
 *
 * <p>将 Servlet 容器提前拦截的超限上传转换为统一业务错误，避免落入系统异常。</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AttachmentController.class)
public class AttachmentExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded() {
        return ResponseEntity.ok(ApiResponse.error(
                ErrorCode.FILE_UPLOAD_FAILED,
                "单个文件不能超过20MB"
        ));
    }
}
