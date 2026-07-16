package com.kinyozi.royale.admin.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class AdminDtos {

    /* ============================ AUTH ============================ */

    public record AdminLoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record AdminLoginResponse(String token, long expiresInMs, String username, String fullName, String role) {}
    public record AdminMeResponse(String username, String email, String fullName, String role) {}

    /* ============================ DASHBOARD ============================ */

    public record DashboardOverview(
            long totalBusinesses, long activeBusinesses, long suspendedBusinesses, long deletedBusinesses,
            long totalOwners, long totalUsers, long totalWorkers, long totalCustomers,
            long businessesCreatedToday, long businessesCreatedThisMonth,
            long sessionsToday, long completedSessionsThisMonth,
            long trialBusinesses, long expiredBusinesses) {}

    /* ============================ BUSINESSES ============================ */

    public record BusinessSummary(
            UUID id, String businessCode, String businessName, String ownerName, String ownerEmail,
            String phone, String status, Instant createdAt,
            long workers, long customers,
            String subscriptionPlan, LocalDate expiryDate, Instant lastLoginAt) {}

    public record BusinessPage(List<BusinessSummary> items, long total, int page, int size) {}

    public record BusinessOwnerDto(UUID id, String username, String email, String role, Instant createdAt) {}

    public record BusinessDetail(
            UUID id, String businessCode, String businessName, String ownerName, String phone,
            String status, Instant createdAt,
            Instant suspendedAt, String suspendedReason,
            String subscriptionPlan, LocalDate expiryDate, Instant lastLoginAt,
            BusinessOwnerDto owner, List<BusinessOwnerDto> users, BusinessStats stats) {}

    public record BusinessStats(
            long workers, long activeWorkers, long customers, long services, long inventoryItems,
            long totalSessions, long completedSessions, long openSessions,
            BigDecimal totalRevenue, BigDecimal creditsOutstanding) {}

    public record WorkerDto(UUID id, String fullName, String phoneNumber, String email, boolean active, Instant createdAt) {}
    public record CustomerDto(UUID id, String name, String phone, Instant createdAt) {}
    public record ServiceDto(UUID id, String name, String category, Integer price, boolean active) {}
    public record InventoryDto(UUID id, String name, String category, Integer currentQty, Integer reorderLevel, Integer pricePerUnit) {}
    public record CreditDto(UUID id, UUID customerId, BigDecimal totalOwed, BigDecimal totalPaid,
                            BigDecimal balance, String status, String note, Instant createdAt) {}
    public record WorkerEarningDto(UUID workerId, String workerName, long sessions, BigDecimal grossRevenue) {}

    public record SuspendRequest(String reason) {}
    public record StatusChangeRequest(String status, String reason) {}
    public record StatusResponse(UUID id, String status, Instant updatedAt) {}

    /* ============================ ANALYTICS ============================ */

    public record TimeSeriesPoint(String bucket, long value) {}
    public record PlatformAnalytics(
            long totalBusinesses, long activeBusinesses, long suspendedBusinesses,
            long totalWorkers, long totalCustomers, long totalSessions, BigDecimal totalRevenue,
            List<TimeSeriesPoint> businessesCreatedDaily, List<TimeSeriesPoint> sessionsDaily) {}

    /* ============================ SUBSCRIPTIONS ============================ */

    public record SubscriptionRow(
            UUID tenantId, String businessName, String businessCode,
            String plan, LocalDate startDate, LocalDate expiryDate, String status,
            BigDecimal lastAmountPaid, String lastPaymentReference,
            Instant lastPaymentAt, Instant updatedAt) {}

    public record SubscriptionSummary(long active, long trial, long suspended, long expired) {}

    /**
     * Full renewal history row — includes every field captured at renewal
     * time so the "View details" dialog on the Subscriptions page can render
     * without extra lookups.
     */
    public record SubscriptionHistoryRow(
            UUID id,
            UUID tenantId,
            String businessName,
            String businessCode,
            String plan,
            LocalDate startDate,
            LocalDate expiryDate,
            LocalDate previousExpiryDate,
            BigDecimal amountPaid,
            String paymentMethod,
            String mpesaReference,
            String paymentReference,
            String paymentNotes,
            String performedBy,
            Instant createdAt) {}

    /** Payload for POST /admin/subscriptions/{tenantId}/renew. */
    public record RenewSubscriptionRequest(
            String plan,
            Integer durationDays,
            LocalDate customExpiryDate,
            BigDecimal amountPaid,
            String paymentMethod,
            String mpesaReference,
            String paymentReference,
            String paymentNotes) {}
}
