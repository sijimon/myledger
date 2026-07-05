package com.myledger.dto;

import java.math.BigDecimal;

/** A labelled monetary total, used for category/project breakdowns. */
public record NameAmount(String name, BigDecimal total) {
}
