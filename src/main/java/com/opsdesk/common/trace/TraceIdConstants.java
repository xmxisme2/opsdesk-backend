package com.opsdesk.common.trace;

/**
 * TraceId 常量。
 *
 * <p>后端日志、响应头和后续异常响应都会使用同一个 TraceId 名称。</p>
 */
public final class TraceIdConstants {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    private TraceIdConstants() {
    }
}

