package com.myledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record FundRequestItemRequest(
        @NotBlank @Size(max = 255) String description,
        @NotNull @Positive BigDecimal qty,
        @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal unitPrice
) {
}
