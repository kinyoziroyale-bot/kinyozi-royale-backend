package com.kinyozi.royale.dto;

import com.kinyozi.royale.model.CommissionType;
import com.kinyozi.royale.model.EmploymentType;
import com.kinyozi.royale.model.SalaryPeriod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class WorkerDto {

    public static class Request {
        @NotBlank public String fullName;
        @NotBlank public String phoneNumber;
        public String email;
        public String profilePhoto;
        @NotEmpty public List<UUID> categoryIds;

        // Payroll (optional; when omitted the worker keeps existing config)
        public EmploymentType employmentType;
        public BigDecimal basicSalary;
        public SalaryPeriod salaryPeriod;
    }

    public static class Response {
        public UUID id;
        public String fullName;
        public String phoneNumber;
        public String email;
        public String profilePhoto;
        public boolean active;
        public List<UUID> categoryIds;
        public List<String> categoryNames;

        // Legacy per-worker commission (kept for backwards-compat)
        public CommissionType commissionType;
        public BigDecimal commissionValue;

        // Payroll
        public EmploymentType employmentType;
        public BigDecimal basicSalary;
        public SalaryPeriod salaryPeriod;

        public Instant createdAt;
        public Instant updatedAt;
    }
}
