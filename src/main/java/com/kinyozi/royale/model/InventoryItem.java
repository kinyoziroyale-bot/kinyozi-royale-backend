package com.kinyozi.royale.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="inventory_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryItem {
    @Id @GeneratedValue private UUID id;
    @Column(nullable=false) private UUID tenantId;
    @Column(nullable=false) private String name;
    @Column private String category;
    @Column private String unit;
    @Builder.Default @Column(nullable=false) private Integer currentQty = 0;
    @Builder.Default @Column(nullable=false) private Integer reorderLevel = 0;
    @Builder.Default @Column(nullable=false) private Integer pricePerUnit = 0;
    @Column private String description;
    @Builder.Default @Column(nullable=false) private Instant createdAt = Instant.now();

    /**
     * Optimistic locking version. Any concurrent update that raced against
     * a stale copy will throw {@link org.springframework.orm.ObjectOptimisticLockingFailureException}
     * instead of silently losing the earlier write.
     */
    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
