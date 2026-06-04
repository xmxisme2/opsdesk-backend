package com.opsdesk.common.exception;

/**
 * 系统统一错误码。
 *
 * <p>错误码与 docs/api-contract.md 保持一致，新增错误码必须同步更新接口契约。</p>
 */
public enum ErrorCode {

    PARAM_ERROR(400001, "请求参数错误"),
    UNAUTHORIZED(401001, "未登录"),
    FORBIDDEN(403001, "无权限"),
    NOT_FOUND(404001, "资源不存在"),
    STATE_CONFLICT(409001, "状态冲突"),
    SYSTEM_ERROR(500001, "系统异常"),
    FILE_UPLOAD_FAILED(500101, "文件上传失败"),
    AI_SERVICE_FAILED(500201, "AI 服务调用失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

