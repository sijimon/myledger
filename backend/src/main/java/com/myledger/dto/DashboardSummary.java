package com.myledger.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Owner dashboard aggregates for a given scope (a financial year, or all time):
 * overall total plus breakdowns by category, project, and month.
 */
public record DashboardSummary(
        Integer financialYear,   // null = all time
        String scopeLabel,       // e.g. "2026-27" or "All time"
        BigDecimal totalSpend,
        long expenseCount,
        List<NameAmount> byCategory,
        List<NameAmount> byProject,
        List<NameAmount> byPhase,
        List<NameAmount> byMonth
) {
}
