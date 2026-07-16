package com.kinyozi.royale.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Users are now identified globally by their email (unique). Username is
 * kept as a display name only and is NOT unique — two businesses may have
 * an owner named "Elvis".
 */
@Entity
@Table(name = "users",
        indexes = { @Index(name = "ix_users_email", columnList = "email") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String username;

    /** Unique across the platform for OWNER / login-capable users. */
    @Column(length = 255)
    private String email;

    /** Owner contact phone (validated at register). */
    @Column(length = 50)
    private String phone;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
