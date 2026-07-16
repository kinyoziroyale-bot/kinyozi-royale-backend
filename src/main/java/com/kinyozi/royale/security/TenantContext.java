package com.kinyozi.royale.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;

/**
 * Holds the current tenant id for the request.
 * Resolution order:
 *   1) Value explicitly set via set(...) (e.g. from a JWT/tenant filter)
 *   2) Authentication principal that exposes getTenantId()
 *   3) Authentication.getDetails() that exposes getTenantId()
 */
public final class TenantContext {
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId) { CURRENT.set(tenantId); }
    public static void clear() { CURRENT.remove(); }

    public static UUID current() {
        UUID t = CURRENT.get();
        if (t != null) return t;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            UUID fromPrincipal = extractTenantId(auth.getPrincipal());
            if (fromPrincipal != null) return fromPrincipal;
            UUID fromDetails = extractTenantId(auth.getDetails());
            if (fromDetails != null) return fromDetails;
        }
        throw new IllegalStateException("Tenant context not set");
    }

    private static UUID extractTenantId(Object obj) {
        if (obj == null) return null;
        try {
            var m = obj.getClass().getMethod("getTenantId");
            Object v = m.invoke(obj);
            if (v instanceof UUID u) return u;
            if (v instanceof String s) return UUID.fromString(s);
        } catch (ReflectiveOperationException ignored) {}
        return null;
    }
}
