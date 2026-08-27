package com.campus.common.trace;

import org.slf4j.MDC;

/**
 * 请求级 TraceId：作为 X-Trace-Id 暴露给客户端 + 写到 MDC 用于日志关联。
 * 当前会话为单进程 v0.1，每次请求生成 16 位 hex。
 */
public final class TraceContext {
    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";
    public static final String ATTR = "com.campus.traceId";

    private TraceContext() {}

    public static String current() {
        String t = MDC.get(MDC_KEY);
        return t == null ? "" : t;
    }
}
