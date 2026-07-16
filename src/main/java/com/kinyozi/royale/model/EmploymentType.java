package com.kinyozi.royale.model;

/**
 * How a worker is paid.
 *   SALARY_ONLY            – flat basic salary, no commissions
 *   COMMISSION_ONLY        – earnings = sum of service commissions
 *   SALARY_PLUS_COMMISSION – basic salary + service commissions
 */
public enum EmploymentType { SALARY_ONLY, COMMISSION_ONLY, SALARY_PLUS_COMMISSION }
