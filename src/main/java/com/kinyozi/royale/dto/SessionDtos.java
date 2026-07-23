package com.kinyozi.royale.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
        /** Nullable — required only when tenant.workerAssignmentMode = BEFORE_CHECKOUT. */
        public UUID workerId;
        /** Agreed price for this transaction. If null, backend snapshots the current service price (legacy behaviour). */
        @PositiveOrZero
        public BigDecimal priceCharged;
    }

    public static class UpdateLineRequest {
        @NotNull public UUID serviceId;
        public UUID workerId;
        @PositiveOrZero
        public BigDecimal priceCharged;
    }

    public static class AssignWorkerRequest {
        /** Nullable — pass null to un-assign. */
        public UUID workerId;
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
        public Boolean hasPendingWorker;
        public List<LineResponse> lines;
    }
}
