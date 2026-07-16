package com.kinyozi.royale.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SessionDtos {

    public static class OpenSessionRequest {
        @NotNull public UUID customerId;
    }

    public static class AddLineRequest {
        @NotNull public UUID serviceId;
        @NotNull public UUID workerId;
        /** Optional override; if null we snapshot the service's current price. */
        public BigDecimal priceCharged;
    }

    public static class UpdateLineRequest {
        @NotNull public UUID serviceId;
        @NotNull public UUID workerId;
        public BigDecimal priceCharged;
    }

    public static class LineResponse {
        public UUID id;
        public UUID serviceId;
        public UUID workerId;
        public BigDecimal priceCharged;
        public Instant startedAt;
        public Instant endedAt;
    }

    public static class SessionResponse {
        public UUID id;
        public UUID customerId;
        public String customerName;
        public String status;
        public Instant openedAt;
        public Instant closedAt;
        public BigDecimal total;
        public List<LineResponse> lines;
    }
}
