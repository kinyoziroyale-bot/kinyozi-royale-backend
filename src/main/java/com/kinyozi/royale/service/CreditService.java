package com.kinyozi.royale.service;

import com.kinyozi.royale.dto.CreditDtos.*;
import com.kinyozi.royale.exception.NotFoundException;
import com.kinyozi.royale.model.Credit;
import com.kinyozi.royale.model.CreditPayment;
import com.kinyozi.royale.repository.CreditPaymentRepository;
import com.kinyozi.royale.repository.CreditRepository;
import com.kinyozi.royale.repository.CustomerRepository;
import com.kinyozi.royale.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CreditService {
    private final CreditRepository credits;
    private final CreditPaymentRepository payments;
    private final CustomerRepository customers;

    public CreditService(CreditRepository c, CreditPaymentRepository p, CustomerRepository customers) {
        this.credits = c;
        this.payments = p;
        this.customers = customers;
    }

    public List<CreditResponse> list(String q) {
        UUID t = CurrentUser.tenantId();
        List<Credit> rows = (q == null || q.isBlank())
                ? credits.findByTenantIdOrderByCreatedAtDesc(t)
                : credits.search(t, "%" + q.toLowerCase() + "%");
        return rows.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public CreditResponse get(UUID id) {
        Credit c = mustOwn(id);
        return toResponse(c);
    }

    @Transactional
    public CreditResponse create(CreateCreditRequest req) {
        if (req.totalOwed == null || req.totalOwed.signum() <= 0)
            throw new IllegalArgumentException("totalOwed must be > 0");
        Credit c = new Credit();
        c.setTenantId(CurrentUser.tenantId());
        c.setCustomerId(req.customerId);
        c.setSessionId(req.sessionId);
        c.setTotalOwed(req.totalOwed);
        c.setTotalPaid(BigDecimal.ZERO);
        c.setStatus("OPEN");
        c.setNote(req.note);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return toResponse(credits.save(c));
    }

    @Transactional
    public PaymentResponse addPayment(UUID creditId, CreditPaymentRequest req) {
        Credit c = mustOwn(creditId);
        if (req.amount == null || req.amount.signum() <= 0)
            throw new IllegalArgumentException("amount must be > 0");

        CreditPayment p = new CreditPayment();
        p.setTenantId(c.getTenantId());
        p.setCreditId(c.getId());
        p.setAmount(req.amount);
        p.setNote(req.note);
        p.setPaidAt(LocalDateTime.now());
        payments.save(p);

        BigDecimal newPaid = c.getTotalPaid().add(req.amount);
        c.setTotalPaid(newPaid);
        if (newPaid.compareTo(c.getTotalOwed()) >= 0) c.setStatus("PAID");
        else c.setStatus("PARTIAL");
        c.setUpdatedAt(LocalDateTime.now());
        credits.save(c);

        return toPaymentResponse(p);
    }

    public List<PaymentResponse> payments(UUID creditId) {
        Credit c = mustOwn(creditId);
        return payments.findByCreditIdOrderByPaidAtDesc(c.getId()).stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(UUID id) {
        Credit c = mustOwn(id);
        payments.findByCreditIdOrderByPaidAtDesc(c.getId()).forEach(payments::delete);
        credits.delete(c);
    }

    private Credit mustOwn(UUID id) {
        Credit c = credits.findById(id).orElseThrow(() -> new NotFoundException("Credit not found"));
        if (!c.getTenantId().equals(CurrentUser.tenantId()))
            throw new NotFoundException("Credit not found");
        return c;
    }

    private CreditResponse toResponse(Credit c) {
        CreditResponse r = new CreditResponse();
        r.id = c.getId();
        r.customerId = c.getCustomerId();

        // 1. Establish fallback values before trying to search the repository
        r.customerName  = "Unknown Customer";
        r.customerPhone = null;

        // 2. Safely fetch customer profile details if ID exists
        if (c.getCustomerId() != null) {
            customers.findById(c.getCustomerId()).ifPresent(cu -> {
                r.customerName  = cu.getName();
                r.customerPhone = cu.getPhone();
            });
        }

        r.sessionId = c.getSessionId();
        r.totalOwed = c.getTotalOwed();
        r.totalPaid = c.getTotalPaid();
        r.balance = c.balance();
        r.status = c.getStatus();
        r.note = c.getNote();
        r.createdAt = c.getCreatedAt();
        r.updatedAt = c.getUpdatedAt();
        r.payments = payments.findByCreditIdOrderByPaidAtDesc(c.getId()).stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
        return r;
    }

    private PaymentResponse toPaymentResponse(CreditPayment p) {
        PaymentResponse pr = new PaymentResponse();
        pr.id = p.getId();
        pr.amount = p.getAmount();
        pr.paidAt = p.getPaidAt();
        pr.note = p.getNote();
        return pr;
    }
}