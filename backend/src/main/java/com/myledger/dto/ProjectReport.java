package com.myledger.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Per-project report for the reports tab. For an owner these figures cover the whole
 * project; for a contractor they cover only that contractor's own contributions.
 */
public record ProjectReport(
        Long projectId,
        String projectName,
        String currency,
        BigDecimal expenseTotal,
        long expenseCount,
        List<NameAmount> expenseByCategory,
        List<NameAmount> expenseByPhase,
        BigDecimal fundRequestTotal,
        long fundRequestCount,
        List<StatusCount> fundRequestByStatus
) {
}
