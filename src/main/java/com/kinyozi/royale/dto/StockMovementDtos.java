package com.kinyozi.royale.dto;
import jakarta.validation.constraints.*;
import java.util.UUID;
public class StockMovementDtos {
  public record CreateRequest(
      @NotNull UUID itemId,
      UUID workerId,
      @NotNull Integer quantity, // positive number; sign decided by reason
      @NotBlank String reason,    // USAGE | RESTOCK | ADJUSTMENT
      String note) {}
}
