package com.opsdesk.common.trace;

/**
 * TraceId 常量。
 *
 * <p>后端日志、响应头和后续异常响应都会使用同一个 TraceId 名称。</p>
 */
public final class TraceIdConstants {

    /** TraceId 响应头名称：用于前后端和网关之间传递同一次请求的追踪编号。 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** TraceId 日志上下文 Key：用于 MDC 中写入日志追踪编号。 */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    private TraceIdConstants() {
    }
}
