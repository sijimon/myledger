package com.myledger.dto;

import java.util.List;

/**
 * The financial years available for filtering: the one containing today, plus every
 * FY that has expenses (current FY always included even if empty).
 */
public record FinancialYearsResponse(FinancialYear current, List<FinancialYear> years) {
}
