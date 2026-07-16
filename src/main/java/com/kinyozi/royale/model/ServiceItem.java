package com.kinyozi.royale.model;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity @Table(name="service_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceItem {
    @Id @GeneratedValue private UUID id;
    @Column(nullable=false) private UUID tenantId;
    @Column(nullable=false) private String name;
    @Column private String category;
    @Builder.Default @Column(nullable=false) private Integer durationMin = 30;
    @Builder.Default @Column(nullable=false) private Integer price = 0;
    @Builder.Default @Column(nullable=false) private Boolean active = true;
    @Builder.Default @Column(nullable=false) private Instant updatedAt = Instant.now();

    /**
     * New commission model — commission is defined per service.
     *  commissionType = PERCENT → commissionValue is 0..100
     *  commissionType = FIXED   → commissionValue is a flat KES amount
     * When null, no commission is paid for this service (falls back to
     * legacy worker-level commission if configured, else 0).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "commission_type")
    private CommissionType commissionType;

    @Column(name = "commission_value", precision = 12, scale = 2)
    private BigDecimal commissionValue;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "service_item_categories",
            joinColumns = @JoinColumn(name = "service_item_id"))
    @Column(name = "category_id")
    @Builder.Default
    private Set<UUID> categoryIds = new HashSet<>();
}
