package com.kinyozi.royale.controller;

import com.kinyozi.royale.dto.AuthDtos.*;
import com.kinyozi.royale.security.CookieAuthSupport;
import com.kinyozi.royale.service.AuthService;
import com.kinyozi.royale.service.AuthService.Login;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    /** Name of the HttpOnly cookie holding the refresh token. */
    public static final String REFRESH_COOKIE = "kr_refresh";

    private final AuthService auth;
    private final CookieAuthSupport cookies;

    public AuthController(AuthService a, CookieAuthSupport c) {
        this.auth = a;
        this.cookies = c;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest r,
                                 HttpServletRequest req,
                                 HttpServletResponse res) {
        Login l = auth.register(r, req);
        cookies.setRefreshCookie(res, l.refreshToken());
        return l.body();
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r,
                              HttpServletRequest req,
                              HttpServletResponse res) {
        Login l = auth.login(r, req);
        cookies.setRefreshCookie(res, l.refreshToken());
        return l.body();
    }

    /**
     * Silent-refresh endpoint. Reads the {@code kr_refresh} HttpOnly cookie,
     * rotates it and returns a fresh access token. No Authorization header
     * required — the refresh cookie is the credential.
     */
    @PostMapping("/refresh")
    public AuthResponse refresh(HttpServletRequest req, HttpServletResponse res) {
        String raw = cookies.readRefreshCookie(req);
        Login l = auth.refresh(raw, req);
        cookies.setRefreshCookie(res, l.refreshToken());
        return l.body();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req, HttpServletResponse res) {
        String raw = cookies.readRefreshCookie(req);
        auth.logout(raw);
        cookies.clearRefreshCookie(res);
        return ResponseEntity.noContent().build();
    }

    /**
     * Change the currently-authenticated user's password. Requires the
     * current password. On success every outstanding session (including
     * this one) is revoked — the caller must sign in again with the new
     * password.
     */
    @PostMapping("/password/change")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordChangeRequest req,
                                               HttpServletResponse res) {
        auth.changePassword(req);
        cookies.clearRefreshCookie(res);
        return ResponseEntity.noContent().build();
    }
}
