package com.kinyozi.royale.security;

import com.kinyozi.royale.controller.AuthController;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Helper for reading and writing the refresh-token cookie. The cookie is
 * always HttpOnly and SameSite. In production ({@code royale.security.cookie.secure=true})
 * it is also flagged Secure — the site MUST be served over HTTPS.
 *
 * The cookie path is scoped to {@code /api/auth} so it is only sent to
 * authentication endpoints, minimising exposure surface.
 */
@Component
public class CookieAuthSupport {

    @Value("${royale.app.refreshExpirationDays:7}")
    private int refreshDays;

    @Value("${royale.security.cookie.secure:false}")
    private boolean secure;

    /** {@code Lax} for same-site/subdomain deployments; set {@code None} for cross-site (requires Secure). */
    @Value("${royale.security.cookie.sameSite:Lax}")
    private String sameSite;

    /** Path the cookie is scoped to. Kept narrow on purpose. */
    @Value("${royale.security.cookie.path:/api/auth}")
    private String path;

    /** Optional explicit cookie domain (leave blank to bind to the request host). */
    @Value("${royale.security.cookie.domain:}")
    private String domain;

    public void setRefreshCookie(HttpServletResponse res, String rawToken) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(AuthController.REFRESH_COOKIE, rawToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(Duration.ofDays(Math.max(1, refreshDays)));
        if (domain != null && !domain.isBlank()) b.domain(domain);
        res.addHeader("Set-Cookie", b.build().toString());
    }

    public void clearRefreshCookie(HttpServletResponse res) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(AuthController.REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(0);
        if (domain != null && !domain.isBlank()) b.domain(domain);
        res.addHeader("Set-Cookie", b.build().toString());
    }

    public String readRefreshCookie(HttpServletRequest req) {
        if (req == null || req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) {
            if (AuthController.REFRESH_COOKIE.equals(c.getName())) {
                String v = c.getValue();
                return (v == null || v.isBlank()) ? null : v;
            }
        }
        return null;
    }
}
