package com.myledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create/update payload for an expense. {@code fileId} is an optional receipt
 * previously returned by the upload endpoint.
 */
public record ExpenseRequest(
        @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal amount,
        @NotNull @PastOrPresent LocalDate expenseDate,
        @NotNull Long categoryId,
        @NotNull Long projectId,
        Long phaseId,
        @Size(max = 160) String vendor,
        String notes,
        Long fileId
) {
}
