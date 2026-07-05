package com.myledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Create/update payload for a project. {@code status} is optional (ACTIVE/ARCHIVED).
 * {@code currency} is an optional ISO 4217 code; defaults to USD on create, left
 * unchanged on update when null.
 */
public record ProjectRequest(
        @NotBlank @Size(max = 120) String name,
        String description,
        LocalDate startDate,
        String status,
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter code") String currency
) {
}
