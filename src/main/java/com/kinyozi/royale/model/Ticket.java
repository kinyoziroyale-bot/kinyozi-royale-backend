package com.kinyozi.royale.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="tickets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticket {
    @Id @GeneratedValue private UUID id;
    @Column(nullable=false) private UUID tenantId;
    @Column private UUID customerId;
    @Column private String customerName;
    @Column private UUID workerId;
    @Column private String workerName;
    @Builder.Default @Column(nullable=false) private String status = "OPEN"; // OPEN|COMPLETED|VOID
    @Builder.Default @Column(nullable=false) private Integer total = 0;
    @Builder.Default @Column(nullable=false) private Instant openedAt = Instant.now();
    @Column private Instant closedAt;
}
