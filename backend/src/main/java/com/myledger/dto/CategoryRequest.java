package com.myledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create/update payload for a category. {@code taxRelevant}/{@code active} are optional;
 * on create they default to false/true, on update null leaves the value unchanged.
 */
public record CategoryRequest(
        @NotBlank @Size(max = 80) String name,
        Boolean taxRelevant,
        Boolean active
) {
}
