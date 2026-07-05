package com.myledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create/update payload for a phase. {@code active} is optional; on create it defaults
 * to true, on update null leaves it unchanged.
 */
public record PhaseRequest(
        @NotBlank @Size(max = 120) String name,
        Boolean active
) {
}
