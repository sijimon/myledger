package com.myledger.dto;

/**
 * A financial year identified by its start year. {@code label} is display text
 * (e.g. "2026-27", or "2026" for a calendar-year FY); {@code current} marks the
 * FY that today falls into.
 */
public record FinancialYear(int value, String label, boolean current) {
}
