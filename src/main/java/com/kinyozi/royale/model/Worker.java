package com.kinyozi.royale.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name="workers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Worker {

    @Id @GeneratedValue private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "phone_number", nullable = false, length = 50)
    private String phoneNumber;

    @Column(length = 255) private String email;
    @Column(name = "profile_photo", length = 1000) private String profilePhoto;

    @Builder.Default @Column(nullable = false) private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "worker_worker_categories",
            joinColumns = @JoinColumn(name = "worker_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    @Builder.Default
    private Set<WorkerCategory> categories = new HashSet<>();

    // --- Legacy per-worker commission (kept for backwards compatibility) ---
    @Enumerated(EnumType.STRING)
    @Column(name = "commission_type")
    private CommissionType commissionType;

    @Column(name = "commission_value", precision = 12, scale = 2)
    private BigDecimal commissionValue;

    // --- New payroll / employment configuration ---
    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type")
    private EmploymentType employmentType;              // SALARY_ONLY / COMMISSION_ONLY / SALARY_PLUS_COMMISSION

    @Column(name = "basic_salary", precision = 12, scale = 2)
    private BigDecimal basicSalary;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_period")
    private SalaryPeriod salaryPeriod;                  // DAILY / WEEKLY / MONTHLY (default MONTHLY)

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate public void onUpdate() { this.updatedAt = Instant.now(); }
}
