package com.myledger.dto;

import com.myledger.entity.FundRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Fund request view. {@code items} is populated on detail views and null in list views.
 */
public record FundRequestResponse(
        Long id,
        Long projectId,
        String projectName,
        String projectCurrency,
        Long requesterId,
        String requesterEmail,
        String title,
        String note,
        String status,
        BigDecimal total,
        OffsetDateTime createdAt,
        List<FundRequestItemResponse> items
) {
    public static FundRequestResponse summary(FundRequest fr) {
        return build(fr, null);
    }

    public static FundRequestResponse detail(FundRequest fr, List<FundRequestItemResponse> items) {
        return build(fr, items);
    }

    private static FundRequestResponse build(FundRequest fr, List<FundRequestItemResponse> items) {
        return new FundRequestResponse(
                fr.getId(),
                fr.getProject().getId(),
                fr.getProject().getName(),
                fr.getProject().getCurrency(),
                fr.getRequester().getId(),
                fr.getRequester().getEmail(),
                fr.getTitle(),
                fr.getNote(),
                fr.getStatus(),
                fr.getTotal(),
                fr.getCreatedAt(),
                items
        );
    }
}
