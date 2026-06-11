package com.opsdesk.common.exception;

/**
 * 系统统一错误码。
 *
 * <p>错误码与 docs/api-contract.md 保持一致，新增错误码必须同步更新接口契约。</p>
 */
public enum ErrorCode {

    /** 参数错误：请求字段缺失、格式错误或取值非法时使用。 */
    PARAM_ERROR(400001, "请求参数错误"),

    /** 未登录：缺少登录态、JWT 无效或登录用户不存在时使用。 */
    UNAUTHORIZED(401001, "未登录"),

    /** 无权限：登录用户缺少角色、权限或资源访问范围时使用。 */
    FORBIDDEN(403001, "无权限"),

    /** 资源不存在：查询、编辑或删除的业务对象不存在时使用。 */
    NOT_FOUND(404001, "资源不存在"),

    /** 状态冲突：业务状态不允许当前操作或唯一约束冲突时使用。 */
    STATE_CONFLICT(409001, "状态冲突"),

    /** 请求过于频繁：命中接口 Redis 限流规则时使用。 */
    REQUEST_TOO_FREQUENT(429001, "操作过于频繁，请稍后再试"),

    /** 系统异常：未预期异常或基础设施故障时使用。 */
    SYSTEM_ERROR(500001, "系统异常"),

    /** 文件上传失败：附件写入、校验或存储失败时使用。 */
    FILE_UPLOAD_FAILED(500101, "文件上传失败"),

    /** AI 服务调用失败：后续真实 AI 接口失败时使用，首版暂不主动触发。 */
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
