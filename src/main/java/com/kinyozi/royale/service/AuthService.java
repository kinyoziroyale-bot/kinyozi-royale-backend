package com.kinyozi.royale.service;

import com.kinyozi.royale.admin.model.TenantAdminMeta;
import com.kinyozi.royale.admin.model.TenantAdminMeta.Status;
import com.kinyozi.royale.admin.repository.TenantAdminMetaRepository;
import com.kinyozi.royale.dto.AuthDtos.*;
import com.kinyozi.royale.exception.BadRequestException;
import com.kinyozi.royale.exception.NotFoundException;
import com.kinyozi.royale.model.*;
import com.kinyozi.royale.repository.*;
import com.kinyozi.royale.security.CurrentUser;
import com.kinyozi.royale.security.JwtUtil;
import com.kinyozi.royale.security.LoginRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/**
 * V5:
 *  - Adds refresh-token issuance and rotation via {@link RefreshTokenService}.
 *    Access tokens are now short-lived; sessions are extended by presenting
 *    the HttpOnly {@code kr_refresh} cookie to /auth/refresh.
 *  - Password change requires the current password AND revokes every
 *    outstanding refresh token so all other sessions are terminated.
 *  - Continues to enforce per-email login rate limiting and generic error
 *    messages against user enumeration.
 */
@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String GENERIC_INVALID = "Invalid email or password";

    private final TenantRepository tenants;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;
    private final TenantAdminMetaRepository meta;
    private final LoginRateLimiter limiter;
    private final RefreshTokenService refreshTokens;
    private final SecureRandom rnd = new SecureRandom();

    private static final Set<Role> ALLOWED_AUX_ROLES = Set.of(Role.MANAGER, Role.CASHIER);
    private static final int DEFAULT_TRIAL_DAYS = 14;

    public AuthService(TenantRepository t, UserRepository u, PasswordEncoder e, JwtUtil j,
                       TenantAdminMetaRepository m, LoginRateLimiter limiter,
                       RefreshTokenService refreshTokens) {
        this.tenants = t; this.users = u; this.encoder = e; this.jwt = j; this.meta = m;
        this.limiter = limiter;
        this.refreshTokens = refreshTokens;
    }

    /** Bundled result: response body + freshly issued raw refresh token to place in cookie. */
    public record Login(AuthResponse body, String refreshToken) {}

    @Transactional
    public Login register(RegisterRequest r, HttpServletRequest http) {
        var owner = r.owner();
        String email = owner.email().trim().toLowerCase();

        if (users.existsByEmailIgnoreCase(email))
            throw new BadRequestException("An account with this email already exists");

        Tenant t = tenants.save(Tenant.builder()
                .businessName(r.business().name())
                .ownerName(owner.username())
                .phone(owner.phone())
                .businessCode(generateUniqueBusinessCode())
                .createdAt(Instant.now())
                .build());

        User u = User.builder()
                .username(owner.username())
                .email(email)
                .phone(owner.phone())
                .passwordHash(encoder.encode(owner.password()))
                .tenantId(t.getId())
                .role(Role.OWNER)
                .createdAt(Instant.now())
                .build();
        users.save(u);

        if (r.auxiliaryUsers() != null) {
            for (var aux : r.auxiliaryUsers()) {
                if (aux == null || aux.username() == null || aux.username().isBlank()
                        || aux.role() == null || aux.role().isBlank()) {
                    throw new BadRequestException("Auxiliary user requires username and role");
                }
                Role auxRole;
                try {
                    auxRole = Role.valueOf(aux.role().trim().toUpperCase());
                } catch (IllegalArgumentException iae) {
                    throw new BadRequestException("Invalid auxiliary user role: " + aux.role());
                }
                if (!ALLOWED_AUX_ROLES.contains(auxRole)) {
                    throw new BadRequestException(
                            "Auxiliary users can only be MANAGER or CASHIER");
                }
                String tempPassword = generateTemporaryPassword();
                User au = User.builder()
                        .tenantId(t.getId())
                        .username(aux.username().trim())
                        .email(null)
                        .passwordHash(encoder.encode(tempPassword))
                        .role(auxRole)
                        .createdAt(Instant.now())
                        .build();
                users.save(au);
                log.info("AUDIT register-aux: tenant={} user={} role={} tempPasswordIssued=true",
                        t.getId(), au.getUsername(), auxRole);
            }
        }

        TenantAdminMeta initialMeta = TenantAdminMeta.builder()
                .tenantId(t.getId())
                .status(Status.TRIAL)
                .subscriptionPlan(r.business().plan())
                .expiryDate(LocalDate.now().plusDays(DEFAULT_TRIAL_DAYS))
                .updatedAt(Instant.now())
                .build();
        meta.save(initialMeta);

        log.info("AUDIT register: business={} tenant={} owner={}",
                t.getBusinessName(), t.getId(), maskEmail(email));

        return buildLogin(u, t, http);
    }

    @Transactional
    public Login login(LoginRequest r, HttpServletRequest http) {
        String email = r.email().trim().toLowerCase();

        long lockedFor = limiter.checkAllowed(email);
        if (lockedFor > 0) {
            log.warn("AUDIT login-blocked (rate-limited): email={} remainingSec={}",
                    maskEmail(email), lockedFor);
            throw new BadRequestException(
                "Too many failed login attempts. Please try again in "
                + Math.max(1, lockedFor / 60) + " minute(s).");
        }

        User u = users.findByEmailIgnoreCase(email).orElse(null);
        if (u == null || !encoder.matches(r.password(), u.getPasswordHash())) {
            limiter.recordFailure(email);
            log.warn("AUDIT login-failed: email={}", maskEmail(email));
            throw new BadRequestException(GENERIC_INVALID);
        }

        Tenant t = tenants.findById(u.getTenantId())
                .orElseThrow(() -> new BadRequestException(GENERIC_INVALID));

        TenantAdminMeta m = meta.findById(t.getId()).orElse(null);
        if (m != null) {
            if (m.getStatus() == Status.SUSPENDED) {
                log.warn("AUDIT login-blocked (suspended): tenant={}", t.getId());
                throw new BadRequestException(
                        "Your Kinyozi Royale account has been suspended. Please contact Kinyozi Royale Support to reactivate your subscription.");
            }
            if (m.getStatus() == Status.DELETED) {
                log.warn("AUDIT login-blocked (deleted): tenant={}", t.getId());
                throw new BadRequestException(
                        "Your Kinyozi Royale account has been closed. Please contact Kinyozi Royale Support.");
            }
            m.setLastLoginAt(Instant.now());
            meta.save(m);
        }

        limiter.recordSuccess(email);
        log.info("AUDIT login-success: tenant={} user={}", t.getId(), u.getUsername());

        return buildLogin(u, t, http);
    }

    /**
     * Consume a refresh token and rotate it. Returns a fresh access token
     * and a new raw refresh token to be re-set as an HttpOnly cookie.
     */
    @Transactional
    public Login refresh(String rawRefreshToken, HttpServletRequest http) {
        RefreshToken row = refreshTokens.lookup(rawRefreshToken);
        if (row == null) throw new BadRequestException("Invalid or expired session");

        User u = users.findById(row.getUserId()).orElse(null);
        if (u == null) {
            refreshTokens.revoke(row);
            throw new BadRequestException("Invalid or expired session");
        }
        Tenant t = tenants.findById(u.getTenantId()).orElse(null);
        if (t == null) throw new BadRequestException("Invalid or expired session");

        TenantAdminMeta m = meta.findById(t.getId()).orElse(null);
        if (m != null && (m.getStatus() == Status.SUSPENDED || m.getStatus() == Status.DELETED)) {
            refreshTokens.revokeAllForUser(u.getId());
            throw new BadRequestException(
                    "Your Kinyozi Royale account is no longer active. Please contact Support.");
        }

        RefreshTokenService.Issued next = refreshTokens.rotate(
                row, u, http == null ? null : http.getHeader("User-Agent"), clientIp(http));
        String accessToken = jwt.generate(u.getUsername(), t.getId().toString(), u.getRole().name());
        AuthResponse body = new AuthResponse(
                accessToken, jwt.expirationSeconds(),
                t.getId().toString(), u.getUsername(), t.getBusinessName(),
                u.getRole().name(), u.getEmail(), u.getPhone(), t.getBusinessCode());
        return new Login(body, next.rawToken());
    }

    /** Revoke a specific refresh token (logout on the current device). */
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        RefreshToken row = refreshTokens.lookup(rawRefreshToken);
        if (row != null) {
            refreshTokens.revoke(row);
            log.info("AUDIT logout: user={}", row.getUserId());
        }
    }

    /**
     * Change the currently-authenticated user's password. Requires the
     * current password AND invalidates every outstanding refresh token so
     * every other session for the user is terminated immediately.
     */
    @Transactional
    public void changePassword(PasswordChangeRequest req) {
        String username = CurrentUser.username();
        if (username == null) throw new BadRequestException("Not authenticated");
        User u = users.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!encoder.matches(req.currentPassword(), u.getPasswordHash())) {
            log.warn("AUDIT password-change-failed: user={}", u.getUsername());
            throw new BadRequestException("Current password is incorrect");
        }
        if (encoder.matches(req.newPassword(), u.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current password");
        }
        u.setPasswordHash(encoder.encode(req.newPassword()));
        users.save(u);
        int revoked = refreshTokens.revokeAllForUser(u.getId());
        log.info("AUDIT password-change: user={} revokedSessions={}", u.getUsername(), revoked);
    }

    // --- helpers -----------------------------------------------------------

    private Login buildLogin(User u, Tenant t, HttpServletRequest http) {
        RefreshTokenService.Issued issued = refreshTokens.issue(u,
                http == null ? null : http.getHeader("User-Agent"), clientIp(http));
        String accessToken = jwt.generate(u.getUsername(), t.getId().toString(), u.getRole().name());
        AuthResponse body = new AuthResponse(
                accessToken, jwt.expirationSeconds(),
                t.getId().toString(), u.getUsername(), t.getBusinessName(),
                u.getRole().name(), u.getEmail(), u.getPhone(), t.getBusinessCode());
        return new Login(body, issued.rawToken());
    }

    private static String clientIp(HttpServletRequest http) {
        if (http == null) return null;
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return http.getRemoteAddr();
    }

    private String generateUniqueBusinessCode() {
        for (int i = 0; i < 12; i++) {
            String code = "KR-" + String.format("%06d", rnd.nextInt(1_000_000));
            if (!tenants.existsByBusinessCode(code)) return code;
        }
        return "KR-" + System.currentTimeMillis();
    }

    private String generateTemporaryPassword() {
        final String alphabet =
                "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        return sb.toString();
    }

    private static String maskEmail(String email) {
        if (email == null) return "null";
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}
