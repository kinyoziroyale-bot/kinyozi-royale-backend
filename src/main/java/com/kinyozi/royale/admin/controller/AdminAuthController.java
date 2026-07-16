package com.kinyozi.royale.admin.controller;

import com.kinyozi.royale.admin.dto.AdminDtos.*;
import com.kinyozi.royale.admin.security.AdminPrincipal;
import com.kinyozi.royale.admin.service.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final AdminAuthService auth;

    public AdminAuthController(AdminAuthService a) { this.auth = a; }

    @PostMapping("/login")
    public AdminLoginResponse login(@Valid @RequestBody AdminLoginRequest r) {
        return auth.login(r);
    }

    @GetMapping("/me")
    public AdminMeResponse me() {
        return auth.me(AdminPrincipal.username());
    }
}
