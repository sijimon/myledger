package com.myledger.dto;

import java.math.BigDecimal;

/** Count and total of fund requests in a given status, for a project report. */
public record StatusCount(String status, Long count, BigDecimal total) {
}
