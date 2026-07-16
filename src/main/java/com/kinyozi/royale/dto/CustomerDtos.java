package com.kinyozi.royale.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Customer-related DTOs. Replace your existing CustomerDtos.java with this file
 * (or merge the isMainCustomer field if you have other DTOs in the same class).
 */
public class CustomerDtos {

    public static class CustomerAnalyticsRow {
        public UUID id;
        public String name;
        public String phone;
        public long visits;
        public BigDecimal totalSpent = BigDecimal.ZERO;
        public LocalDateTime lastVisit;
        public LocalDate nextVisitDate;
        public boolean isMainCustomer;
    }

    public static class ReminderRow {
        public UUID id;
        public String name;
        public String phone;
        public LocalDate nextVisitDate;
        public String bucket;
    }

    public static class UpdateNextVisitRequest {
        public LocalDate nextVisitDate;
        public String notes;
    }
}
