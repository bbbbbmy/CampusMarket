package com.campus.common.auth;

import com.campus.common.error.BusinessException;
import com.campus.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;

/**
 * v0.1 简化鉴权：从 request attribute 取当前 userId / schoolId，
 * 由 AuthInterceptor 写入。不引 spring-security。
 */
public final class AuthContext {
    public static final String ATTR_USER_ID = "com.campus.userId";
    public static final String ATTR_SCHOOL_ID = "com.campus.schoolId";

    private AuthContext() {}

    public static long requireUserId(HttpServletRequest req) {
        Object v = req.getAttribute(ATTR_USER_ID);
        if (v == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "missing token");
        return (Long) v;
    }

    public static long requireSchoolId(HttpServletRequest req) {
        Object v = req.getAttribute(ATTR_SCHOOL_ID);
        if (v == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "missing token");
        return (Long) v;
    }
}
