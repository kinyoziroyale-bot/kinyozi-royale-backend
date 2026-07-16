package com.kinyozi.royale.admin.service;

import com.kinyozi.royale.admin.dto.AdminDtos.*;
import com.kinyozi.royale.admin.model.PlatformAdmin;
import com.kinyozi.royale.admin.repository.PlatformAdminRepository;
import com.kinyozi.royale.admin.security.AdminJwtUtil;
import com.kinyozi.royale.exception.BadRequestException;
import com.kinyozi.royale.exception.NotFoundException;
import com.kinyozi.royale.security.LoginRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Platform-admin authentication.
 *
 * <p>Hardening (M-1): the same {@link LoginRateLimiter} that protects the
 * tenant login now also gates {@code /api/admin/auth/login}. Failed
 * attempts are counted per lower-cased admin username under a distinct
 * {@code admin:} key namespace so tenant email lockouts never collide
 * with admin username lockouts. Successful logins reset the counter.
 *
 * <p>Error messages remain generic ("Invalid credentials") so the
 * endpoint cannot be used to enumerate valid admin usernames.
 *
 * <p>MFA readiness: the {@link PlatformAdmin} record deliberately holds
 * no MFA columns yet. When TOTP is added, plug it in between the password
 * check and the JWT issuance below — the rate limiter, audit log, and
 * generic error contract can stay unchanged.
 */
@Service
public class AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthService.class);
    private static final String GENERIC_INVALID = "Invalid credentials";

    private final PlatformAdminRepository admins;
    private final PasswordEncoder encoder;
    private final AdminJwtUtil jwt;
    private final LoginRateLimiter limiter;

    public AdminAuthService(PlatformAdminRepository admins,
                            PasswordEncoder encoder,
                            AdminJwtUtil jwt,
                            LoginRateLimiter limiter) {
        this.admins = admins;
        this.encoder = encoder;
        this.jwt = jwt;
        this.limiter = limiter;
    }

    private static String rateKey(String username) {
        return "admin:" + (username == null ? "" : username.trim().toLowerCase());
    }

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest req) {
        String username = req.username() == null ? "" : req.username().trim();
        String key = rateKey(username);

        long lockedFor = limiter.checkAllowed(key);
        if (lockedFor > 0) {
            log.warn("AUDIT admin-login-blocked (rate-limited): username={} remainingSec={}",
                    username, lockedFor);
            throw new BadRequestException(
                "Too many failed login attempts. Please try again in "
                + Math.max(1, lockedFor / 60) + " minute(s).");
        }

        PlatformAdmin a = admins.findByUsername(username).orElse(null);
        if (a == null || !a.isActive() || !encoder.matches(req.password(), a.getPasswordHash())) {
            limiter.recordFailure(key);
            log.warn("AUDIT admin-login-failed: username={}", username);
            throw new BadRequestException(GENERIC_INVALID);
        }

        limiter.recordSuccess(key);
        a.setLastLoginAt(Instant.now());
        admins.save(a);
        String token = jwt.generate(a.getUsername());
        log.info("AUDIT admin-login-success: username={}", a.getUsername());
        return new AdminLoginResponse(
                token, jwt.expirationMs(),
                a.getUsername(), a.getFullName(),
                AdminJwtUtil.ROLE_PLATFORM_ADMIN);
    }

    public AdminMeResponse me(String username) {
        PlatformAdmin a = admins.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Admin not found"));
        return new AdminMeResponse(
                a.getUsername(), a.getEmail(), a.getFullName(),
                AdminJwtUtil.ROLE_PLATFORM_ADMIN);
    }
}
