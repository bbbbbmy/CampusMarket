package com.campus.common.error;

import com.campus.common.api.ApiResponse;
import com.campus.common.trace.TraceContext;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一异常处理。把已知业务异常映射到错误码 + HTTP 状态；
 * 把 422 / 校验错误统一格式化；未知异常返回 500 + INTERNAL。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        ErrorCode ec = ex.getErrorCode();
        return ResponseEntity
            .status(ec.http())
            .body(ApiResponse.fail(ec.code(), ex.getMessage(), TraceContext.current()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String first = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(e -> e.getField() + " " + e.getDefaultMessage())
            .orElse("invalid request");
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiResponse.fail(ErrorCode.BAD_REQUEST.code(), first, TraceContext.current()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiResponse.fail(ErrorCode.BAD_REQUEST.code(), ex.getMessage(), TraceContext.current()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAny(Exception ex) {
        // v0.1 debug: 暴露 root cause 短描述，便于错误调试；production 应改为脱敏 + 仅日志
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String msg = (root.getMessage() == null ? ex.getClass().getSimpleName() : root.getMessage());
        if (msg.length() > 200) msg = msg.substring(0, 200);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.fail("INTERNAL", msg, TraceContext.current()));
    }
}
