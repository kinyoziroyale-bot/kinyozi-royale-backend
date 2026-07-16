package com.kinyozi.royale.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name="customers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer {
    @Id @GeneratedValue private UUID id;
    @Column(nullable=false) private UUID tenantId;
    @Column(nullable=false) private String name;
    @Column private String phone;

    @Column(name="next_visit_date")
    private LocalDate nextVisitDate;

    @Column(columnDefinition="text")
    private String notes;

    @Builder.Default @Column(nullable=false) private Instant createdAt = Instant.now();
//
//    @Builder.Default
//    @Column(name = "main_customer", nullable = false)
//    private boolean mainCustomer = false;

    @Builder.Default
    @Column(name="main_customer", nullable=false,
            columnDefinition="boolean not null default false")
    private boolean mainCustomer = false;

}
