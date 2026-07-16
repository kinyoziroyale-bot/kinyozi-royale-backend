package com.kinyozi.royale.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "session_lines")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SessionLine {

    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private CustomerSession session;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "worker_id", nullable = false)
    private UUID workerId;

    @Column(name = "price_charged", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceCharged;

    @Builder.Default
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;
}
