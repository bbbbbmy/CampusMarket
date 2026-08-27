package com.campus.common.auth;

import com.campus.common.jwt.JwtSupport;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 拦截除白名单以外的请求，从 Authorization: Bearer 抽 JWT 写入请求属性。
 * 白名单：注册 / 登录 / 健康检查 / OpenAPI。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** 公开路径前缀（v0.1 内联；v0.2 改用配置 / Nacos） */
    public static final Set<String> PUBLIC_PREFIX = Set.of(
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/api/v1/schools",
        "/api/v1/categories",
        "/healthz",
        "/actuator"
    );

    private static final Pattern BEARER = Pattern.compile("^Bearer\\s+(.+)$");

    private final JwtSupport jwt;
    private final boolean enabled;

    public AuthInterceptor(JwtSupport jwt,
                           @Value("${campus.auth.enabled:true}") boolean enabled) {
        this.jwt = jwt;
        this.enabled = enabled;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if (!enabled) return true;
        String path = req.getRequestURI();
        for (String prefix : PUBLIC_PREFIX) {
            if (path.startsWith(prefix)) return true;
        }
        String h = req.getHeader("Authorization");
        if (h == null || !BEARER.matcher(h).matches()) {
            throw new com.campus.common.error.BusinessException(
                com.campus.common.error.ErrorCode.UNAUTHORIZED, "missing bearer token");
        }
        String token = BEARER.matcher(h).replaceFirst("$1").trim();
        try {
            Claims c = jwt.parse(token);
            req.setAttribute(AuthContext.ATTR_USER_ID, jwt.userId(c));
            req.setAttribute(AuthContext.ATTR_SCHOOL_ID, jwt.schoolId(c));
        } catch (Exception ex) {
            throw new com.campus.common.error.BusinessException(
                com.campus.common.error.ErrorCode.UNAUTHORIZED, "invalid token");
        }
        return true;
    }
}
