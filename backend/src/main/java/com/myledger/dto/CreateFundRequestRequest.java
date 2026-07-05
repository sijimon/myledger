package com.myledger.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Create a fund request, optionally with its line items in one call, and optionally
 * submit it immediately (requires at least one item).
 */
public record CreateFundRequestRequest(
        @NotNull Long projectId,
        @NotBlank @Size(max = 160) String title,
        String note,
        @Valid List<FundRequestItemRequest> items,
        boolean submit
) {
}
