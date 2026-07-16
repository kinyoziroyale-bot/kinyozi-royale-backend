package com.kinyozi.royale.dto;

import com.kinyozi.royale.model.EmploymentType;
import com.kinyozi.royale.model.SalaryPeriod;

import java.math.BigDecimal;
import java.util.UUID;

public class EarningsDtos {

    /**
     * Per-worker earnings breakdown returned by /earnings/*.
     * Legacy fields (workerId, workerName, sessions, services, earnings) are
     * preserved so existing UIs keep working. New fields expose the payroll
     * model and business-profit numbers.
     */
    public static class WorkerEarnings {
        public UUID workerId;
        public String workerName;
        public long sessions;
        public long services;

        /**
         * TOTAL earnings for the worker in the period (basicSalary +
         * commission). Kept named "earnings" for backwards compatibility.
         */
        public BigDecimal earnings;

        // New payroll breakdown
        public EmploymentType employmentType;
        public SalaryPeriod   salaryPeriod;
        public BigDecimal     basicSalary;      // pro-rated to the period
        public BigDecimal     commission;       // sum of per-service commissions
        public BigDecimal     totalSales;       // gross sales the worker generated
        public BigDecimal     averageEarnings;  // earnings / services
        public String         topServiceName;   // highest-grossing service performed
    }

    /** Business-wide summary for the same period. */
    public static class BusinessSummary {
        public BigDecimal totalSales;          // sum of every completed line
        public BigDecimal totalCommission;     // total paid out as commission
        public BigDecimal totalSalary;         // pro-rated basic salaries
        public BigDecimal totalPayout;         // commission + salary
        public BigDecimal businessGrossProfit; // totalSales - totalCommission
        public BigDecimal businessNetProfit;   // totalSales - totalPayout
        public long       sessionsCount;
        public long       servicesCount;
    }
}
