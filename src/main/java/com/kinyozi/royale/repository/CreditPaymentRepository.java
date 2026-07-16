package com.kinyozi.royale.repository;

import com.kinyozi.royale.model.CreditPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CreditPaymentRepository extends JpaRepository<CreditPayment, UUID> {
    List<CreditPayment> findByCreditIdOrderByPaidAtDesc(UUID creditId);
}
