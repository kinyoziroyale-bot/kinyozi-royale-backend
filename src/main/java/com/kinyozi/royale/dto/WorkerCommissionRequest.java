package com.kinyozi.royale.dto;
import com.kinyozi.royale.model.CommissionType;
import java.math.BigDecimal;

public record WorkerCommissionRequest(CommissionType commissionType,
                                      BigDecimal commissionValue) {}
