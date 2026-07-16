package com.kinyozi.royale.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Server-side refresh token record. The raw refresh token value is NEVER
 * stored — only its SHA-256 hash. Tokens are single-use: on refresh the
 * matching row is marked {@code revoked=true} and a fresh row is issued
 * (rotation). Password change, logout and role change all revoke every
 * outstanding row for the user.
 */
@Entity
@Table(name = "refresh_tokens",
        indexes = {
                @Index(name = "ix_refresh_tokens_hash", columnList = "token_hash", unique = true),
                @Index(name = "ix_refresh_tokens_user", columnList = "user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** SHA-256 hex of the raw token. Unique. */
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Optional link to the token that superseded this one during rotation. */
    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "user_agent", length = 256)
    private String userAgent;

    @Column(name = "ip", length = 64)
    private String ip;
}
