package com.kinyozi.royale.service;

import com.kinyozi.royale.model.RefreshToken;
import com.kinyozi.royale.model.User;
import com.kinyozi.royale.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Refresh token lifecycle. Raw tokens are 256-bit random values, URL-safe
 * base64 encoded, and NEVER stored — only their SHA-256 hash is persisted.
 * Every successful refresh rotates the token; the previous row is marked
 * revoked and linked to its replacement for forensic tracing.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom RNG = new SecureRandom();

    private final RefreshTokenRepository repo;
    private final Duration ttl;

    public RefreshTokenService(RefreshTokenRepository repo,
                               @Value("${royale.app.refreshExpirationDays:7}") int refreshDays) {
        this.repo = repo;
        this.ttl = Duration.ofDays(Math.max(1, refreshDays));
    }

    public Duration ttl() { return ttl; }

    /** Result: the freshly-issued raw token (to hand to the client) and its DB row. */
    public record Issued(String rawToken, RefreshToken row) {}

    @Transactional
    public Issued issue(User user, String userAgent, String ip) {
        byte[] rnd = new byte[32];
        RNG.nextBytes(rnd);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
        RefreshToken row = RefreshToken.builder()
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .tokenHash(sha256(raw))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(ttl))
                .userAgent(trunc(userAgent, 256))
                .ip(trunc(ip, 64))
                .build();
        repo.save(row);
        return new Issued(raw, row);
    }

    /**
     * Validate a raw refresh token and, if valid, rotate it — returning the
     * new raw token plus the underlying User row. Returns null if the token
     * is unknown, expired or already revoked (do NOT leak which).
     */
    @Transactional
    public RefreshToken lookup(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return null;
        RefreshToken row = repo.findByTokenHash(sha256(rawToken)).orElse(null);
        if (row == null) return null;
        if (row.isRevoked()) {
            // Presenting a revoked token is highly suspicious — revoke every
            // active token for this user as a defensive containment measure.
            log.warn("AUDIT refresh-reuse: user={} — revoking all sessions", row.getUserId());
            repo.revokeAllForUser(row.getUserId(), Instant.now());
            return null;
        }
        if (row.getExpiresAt().isBefore(Instant.now())) return null;
        return row;
    }

    @Transactional
    public Issued rotate(RefreshToken current, User user, String userAgent, String ip) {
        Issued next = issue(user, userAgent, ip);
        current.setRevoked(true);
        current.setRevokedAt(Instant.now());
        current.setReplacedBy(next.row().getId());
        repo.save(current);
        return next;
    }

    @Transactional
    public void revoke(RefreshToken row) {
        if (row == null || row.isRevoked()) return;
        row.setRevoked(true);
        row.setRevokedAt(Instant.now());
        repo.save(row);
    }

    /**
     * Revoke every outstanding refresh token for a user. Called on logout,
     * password change and role change so those actions immediately end
     * every existing session.
     */
    @Transactional
    public int revokeAllForUser(java.util.UUID userId) {
        return repo.revokeAllForUser(userId, Instant.now());
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String trunc(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
