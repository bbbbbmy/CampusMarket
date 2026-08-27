package com.campus.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 进入 filter 时：若无 X-Trace-Id，生成 UUID.hex()；写入 MDC + request attribute
 * + response header。链路结束清理 MDC。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String incoming = req.getHeader(TraceContext.HEADER);
        String traceId = (incoming == null || incoming.isBlank())
            ? UUID.randomUUID().toString().replace("-", "").substring(0, 16)
            : incoming;
        MDC.put(TraceContext.MDC_KEY, traceId);
        req.setAttribute(TraceContext.ATTR, traceId);
        res.setHeader(TraceContext.HEADER, traceId);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(TraceContext.MDC_KEY);
        }
    }
}
