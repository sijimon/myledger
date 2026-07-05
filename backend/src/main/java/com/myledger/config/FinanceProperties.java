package com.myledger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code myledger.finance.*}.
 *
 * @param fyStartMonth the month (1–12) a financial year begins. Default 4 (April),
 *                     the Indian FY. Set to 1 for a calendar-year FY.
 */
@ConfigurationProperties(prefix = "myledger.finance")
public record FinanceProperties(int fyStartMonth) {

    public FinanceProperties {
        if (fyStartMonth < 1 || fyStartMonth > 12) {
            throw new IllegalArgumentException("fy-start-month must be 1..12");
        }
    }
}
