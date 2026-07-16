package com.kinyozi.royale.admin.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT helper for the Developer Portal. Uses a distinct secret and a
 * dedicated audience ("platform-admin") so admin tokens can never be
 * mistaken for tenant tokens even if secrets are misconfigured.
 */
@Component
public class AdminJwtUtil {

    private static final Logger log = LoggerFactory.getLogger(AdminJwtUtil.class);

    public static final String AUDIENCE = "platform-admin";
    public static final String CLAIM_ROLE = "role";
    public static final String ROLE_PLATFORM_ADMIN = "PLATFORM_ADMIN";

    private final String secret;
    private final long expirationMs;

    public AdminJwtUtil(
            @Value("${royale.admin.jwtSecret}") String secret,
            @Value("${royale.admin.jwtExpirationMs:86400000}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    @PostConstruct
    void validate() {
        if (secret == null || secret.isBlank()
                || secret.startsWith("CHANGE_ME_INSECURE_ADMIN_DEFAULT")
                || secret.length() < 32) {
            throw new IllegalStateException(
                    "ROYALE_ADMIN_JWT_SECRET is missing, too short (< 32 chars), or set to the "
                    + "insecure default. Set the ROYALE_ADMIN_JWT_SECRET environment variable "
                    + "to a strong random value distinct from JWT_SECRET before starting.");
        }
        log.info("Admin JWT signing secret loaded ({} chars).", secret.length());
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .audience().add(AUDIENCE).and()
                .claims(Map.of(CLAIM_ROLE, ROLE_PLATFORM_ADMIN))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key())
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .requireAudience(AUDIENCE)
                .build()
                .parseSignedClaims(token);
    }

    public long expirationMs() { return expirationMs; }
}
