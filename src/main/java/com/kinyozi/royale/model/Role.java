package com.kinyozi.royale.model;

/**
 * Tenant-scoped roles. OWNER is the highest tenant role; platform-level
 * administration lives in the separate {@code admin} package
 * ({@code PlatformAdmin}) and is never granted to a tenant user.
 *
 * <p>{@link #ADMIN} is <strong>reserved / not issued</strong>. It remains
 * declared only so historical rows that may already contain the string
 * "ADMIN" continue to deserialize cleanly. Do NOT reference it in new
 * authorisation checks — use OWNER (and MANAGER where staff access is
 * appropriate).
 */
public enum Role {
    OWNER,
    /** @deprecated Reserved for historical rows only. Not granted anywhere. */
    @Deprecated
    ADMIN,
    MANAGER,
    CASHIER
}
