package com.campus.common.api;

/**
 * 统一响应体：{code, message, data, traceId}。
 * 成功时 code="OK"；失败时由 GlobalExceptionHandler 写入 ErrorCode。
 */
public final class ApiResponse<T> {
    public static final String OK = "OK";

    private final String code;
    private final String message;
    private final T data;
    private final String traceId;

    private ApiResponse(String code, String message, T data, String traceId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(OK, "success", data, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId);
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public String getTraceId() { return traceId; }
}
