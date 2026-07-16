package com.kinyozi.royale.security;

import com.kinyozi.royale.admin.model.TenantAdminMeta;
import com.kinyozi.royale.admin.model.TenantAdminMeta.Status;
import com.kinyozi.royale.admin.repository.TenantAdminMetaRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Tenant JWT filter.
 *
 * V4:
 *  - Subscription expiry / account status enforcement (unchanged from V3).
 *  - Malformed / expired / bad-signature tokens now respond with a
 *    401 INVALID_TOKEN JSON body instead of being silently ignored,
 *    so the frontend can force re-login.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtUtil jwt;
    private final TenantAdminMetaRepository metaRepo;

    public JwtFilter(JwtUtil jwt, TenantAdminMetaRepository metaRepo) {
        this.jwt = jwt;
        this.metaRepo = metaRepo;
    }

    private static boolean isExpiryAllowlisted(String path) {
        if (path == null) return false;
        return path.startsWith("/auth/")
                || path.equals("/subscription/me")
                || path.equals("/health");
    }

    private static boolean isPublicPath(String path) {
        if (path == null) return false;
        return path.startsWith("/auth/") || path.equals("/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String path = req.getRequestURI();
        // Strip context-path (/api) if present so the internal path matches
        // the security-config matchers ("/auth/**", "/health").
        String ctx = req.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path != null && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }

        if (path != null && path.startsWith("/admin/")) {
            chain.doFilter(req, res);
            return;
        }

        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            String token = h.substring(7);
            Claims c;
            try {
                c = jwt.parse(token).getPayload();
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                writeInvalidToken(res, "TOKEN_EXPIRED", "Your session has expired. Please sign in again.");
                return;
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
                log.debug("Rejected malformed/invalid JWT: {}", e.getClass().getSimpleName());
                writeInvalidToken(res, "INVALID_TOKEN", "Your session is invalid. Please sign in again.");
                return;
            }

            String username = c.getSubject();
            String role = (String) c.get("role");
            String tenantId = (String) c.get("tenantId");

            if (tenantId != null) {
                try {
                    TenantAdminMeta m = metaRepo.findById(UUID.fromString(tenantId)).orElse(null);
                    if (m != null) {
                        if (m.getStatus() == Status.SUSPENDED || m.getStatus() == Status.DELETED) {
                            writeSuspended(res, m.getStatus(), m.getSuspendedReason());
                            return;
                        }
                        if (!isExpiryAllowlisted(path)
                                && m.getExpiryDate() != null
                                && LocalDate.now().isAfter(m.getExpiryDate())) {
                            writeExpired(res);
                            return;
                        }
                    }
                } catch (IllegalArgumentException ignored) { /* not a uuid */ }
            }

            var auth = new UsernamePasswordAuthenticationToken(username, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + (role == null ? "OWNER" : role))));
            auth.setDetails(tenantId);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else if (!isPublicPath(path) && !(path != null && "OPTIONS".equalsIgnoreCase(req.getMethod()))) {
            // No token on a protected path — let Spring Security return the default 401.
        }
        chain.doFilter(req, res);
    }

    private void writeInvalidToken(HttpServletResponse res, String code, String message) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(
                "{\"code\":\"" + code + "\"," +
                        "\"error\":\"Unauthorized\"," +
                        "\"message\":\"" + message + "\"}");
    }

    private void writeSuspended(HttpServletResponse res, Status status, String reason) throws IOException {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        res.setContentType("application/json;charset=UTF-8");
        String msg = status == Status.DELETED
                ? "Your Kinyozi Royale account has been closed. Please contact Kinyozi Royale Support."
                : "Your Kinyozi Royale account has been suspended. Please contact Kinyozi Royale Support to reactivate your subscription.";
        String reasonJson = reason == null ? "null" : "\"" + reason.replace("\"", "\\\"") + "\"";
        res.getWriter().write(
                "{\"code\":\"ACCOUNT_" + status.name() + "\"," +
                        "\"error\":\"ACCOUNT_" + status.name() + "\"," +
                        "\"message\":\"" + msg + "\"," +
                        "\"reason\":" + reasonJson + "}");
    }

    private void writeExpired(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(
                "{\"code\":\"SUBSCRIPTION_EXPIRED\"," +
                        "\"error\":\"SUBSCRIPTION_EXPIRED\"," +
                        "\"message\":\"Your subscription has expired. Please renew it to continue using Kinyozi Royale.\"}");
    }
}
