package com.kinyozi.royale.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "worker_categories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "name"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkerCategory {
    @Id @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @ManyToMany(mappedBy = "categories")
    @Builder.Default
    private Set<Worker> workers = new HashSet<>();
}
