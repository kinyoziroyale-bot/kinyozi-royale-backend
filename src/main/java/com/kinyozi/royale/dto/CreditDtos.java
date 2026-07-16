package com.kinyozi.royale.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CreditDtos {

    public static class CreateCreditRequest {
        public UUID customerId;
        public UUID sessionId;       // optional
        public BigDecimal totalOwed;
        public String note;
    }

    public static class CreditPaymentRequest {
        public BigDecimal amount;
        public String note;
    }

    public static class PaymentResponse {
        public UUID id;
        public BigDecimal amount;
        public LocalDateTime paidAt;
        public String note;
    }



    public static class CreditResponse {
        public UUID id;
        public UUID customerId;
        public String customerName;     // NEW
        public String customerPhone;    // NEW
        public UUID sessionId;
        public BigDecimal totalOwed;
        public BigDecimal totalPaid;
        public BigDecimal balance;
        public String status;
        public String note;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
        public List<PaymentResponse> payments;
    }
}
