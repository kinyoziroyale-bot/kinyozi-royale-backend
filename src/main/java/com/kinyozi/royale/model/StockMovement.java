package com.kinyozi.royale.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="stock_movements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockMovement {
    @Id @GeneratedValue private UUID id;
    @Column(nullable=false) private UUID tenantId;
    @Column(nullable=false) private UUID itemId;
    @Column(nullable=false) private String itemName;
    @Column private UUID workerId;
    @Column private String workerName;
    @Column(nullable=false) private Integer quantity; // signed: + restock, - usage
    @Column private String unit;
    @Column private String reason; // RESTOCK or USAGE
    @Column private String note;
    @Builder.Default
    @Column(nullable=false) private Instant createdAt = Instant.now();
}
