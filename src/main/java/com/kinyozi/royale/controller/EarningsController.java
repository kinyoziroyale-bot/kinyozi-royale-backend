package com.kinyozi.royale.controller;

import com.kinyozi.royale.dto.EarningsDtos.BusinessSummary;
import com.kinyozi.royale.dto.EarningsDtos.WorkerEarnings;
import com.kinyozi.royale.service.EarningsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/earnings")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class EarningsController {

    private final EarningsService svc;
    public EarningsController(EarningsService s) { this.svc = s; }

    @GetMapping("/daily")
    public List<WorkerEarnings> daily(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return svc.daily(date);
    }

    @GetMapping("/weekly")
    public List<WorkerEarnings> weekly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return svc.range(from, to);
    }

    @GetMapping("/monthly")
    public List<WorkerEarnings> monthly(@RequestParam String month) {
        return svc.monthly(YearMonth.parse(month));
    }

    @GetMapping("/total")
    public List<WorkerEarnings> total() { return svc.total(); }

    @GetMapping("/worker/{workerId}")
    public WorkerEarnings forWorker(@PathVariable UUID workerId) { return svc.forWorker(workerId); }

    // ---------- Business-wide summary (sales, commission, salary, profit) ----------

    @GetMapping("/summary/daily")
    public BusinessSummary summaryDaily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return svc.summaryDaily(date);
    }

    @GetMapping("/summary/range")
    public BusinessSummary summaryRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return svc.summaryRange(from, to);
    }

    @GetMapping("/summary/monthly")
    public BusinessSummary summaryMonthly(@RequestParam String month) {
        return svc.summaryMonthly(YearMonth.parse(month));
    }

    @GetMapping("/summary/total")
    public BusinessSummary summaryTotal() { return svc.summaryTotal(); }
}
