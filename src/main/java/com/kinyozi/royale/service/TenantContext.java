package com.kinyozi.royale.service;

import com.kinyozi.royale.security.CurrentUser;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;

/**
 * Resolves the current tenantId for the new (session / earnings / worker)
 * feature modules. It delegates to the existing {@link CurrentUser} helper,
 * which reads the tenantId stamped on the SecurityContext by {@code JwtFilter}.
 *
 * Keeping this thin adapter means the pasted feature code (which calls
 * {@code TenantContext.current()}) stays untouched while still using the
 * project's single source of truth for tenant resolution.
 */
public final class TenantContext {
    private TenantContext() {}

    public static UUID current() {
        return CurrentUser.tenantId();
    }

    /** Implement this on an auth principal class that natively carries the tenant. */
    public interface TenantAware { UUID getTenantId(); }
}
