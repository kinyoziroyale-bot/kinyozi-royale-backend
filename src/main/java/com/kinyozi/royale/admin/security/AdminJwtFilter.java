package com.kinyozi.royale.admin.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Validates Developer-Portal JWTs on every {@code /admin/**} request.
 * Only applied by {@link AdminSecurityConfig} so the tenant filter chain
 * (which continues to use {@code JwtFilter}) is not affected.
 */
@Component
public class AdminJwtFilter extends OncePerRequestFilter {

    private final AdminJwtUtil jwt;

    public AdminJwtFilter(AdminJwtUtil jwt) { this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            try {
                Claims c = jwt.parse(h.substring(7)).getPayload();
                String username = c.getSubject();
                String role = (String) c.get(AdminJwtUtil.CLAIM_ROLE);
                if (username != null && AdminJwtUtil.ROLE_PLATFORM_ADMIN.equals(role)) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            username, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + AdminJwtUtil.ROLE_PLATFORM_ADMIN)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                // fall through — endpoint will reject as unauthorised
            }
        }
        chain.doFilter(req, res);
    }
}
