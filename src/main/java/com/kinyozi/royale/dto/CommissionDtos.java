package com.kinyozi.royale.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class CommissionDtos {
    public static class CommissionRequest {
        public UUID workerId;
        public UUID serviceId; // optional
        public BigDecimal percent;
        public BigDecimal fixedAmount;
        public Boolean active;
    }
    public static class CommissionResponse {
        public UUID id;
        public UUID workerId;
        public String workerName;
        public UUID serviceId;
        public String serviceName;
        public BigDecimal percent;
        public BigDecimal fixedAmount;
        public boolean active;
    }
}
