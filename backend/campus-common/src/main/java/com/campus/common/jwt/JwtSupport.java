package com.campus.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 签发与解析。HS256，secret 通过 campus.jwt.secret 注入。
 * Claims：sub=userId, schoolId, jti=uuid。
 */
@Component
public class JwtSupport {

    private final SecretKey key;
    private final Duration ttl;

    public JwtSupport(
        @Value("${campus.jwt.secret:demo-secret-key-must-be-at-least-32-bytes-long-for-hs256}") String secret,
        @Value("${campus.jwt.ttl-hours:24}") long ttlHours
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofHours(ttlHours);
    }

    public String issue(long userId, long schoolId) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("schoolId", schoolId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(ttl)))
            .signWith(key)
            .compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException e) {
            throw new IllegalArgumentException("invalid token: " + e.getMessage(), e);
        }
    }

    public long userId(Claims c) { return Long.parseLong(c.getSubject()); }
    public long schoolId(Claims c) { return c.get("schoolId", Number.class).longValue(); }
}
