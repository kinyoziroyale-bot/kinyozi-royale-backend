package com.kinyozi.royale.repository;
import com.kinyozi.royale.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    List<Customer> findByTenantId(UUID tenantId);

    List<Customer> findByTenantIdAndNextVisitDate(UUID tenantId, LocalDate date);

    @Query("select c from Customer c where c.tenantId = :t " +
            "and c.nextVisitDate is not null " +
            "and c.nextVisitDate between :from and :to")
    List<Customer> findUpcoming(@Param("t") UUID tenantId,
                                @Param("from") LocalDate from,
                                @Param("to") LocalDate to);

    @Query("select c from Customer c where c.tenantId = :t " +
            "and c.nextVisitDate is not null and c.nextVisitDate < :today")
    List<Customer> findOverdue(@Param("t") UUID tenantId, @Param("today") LocalDate today);


}
