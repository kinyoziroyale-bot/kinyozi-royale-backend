package com.kinyozi.royale.admin.security;

import org.springframework.security.core.context.SecurityContextHolder;

public final class AdminPrincipal {
    private AdminPrincipal() {}
    public static String username() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? null : (String) a.getPrincipal();
    }
}
