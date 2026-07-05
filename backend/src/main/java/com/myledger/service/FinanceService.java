package com.myledger.service;

import com.myledger.config.FinanceProperties;
import com.myledger.dto.FinancialYear;
import com.myledger.dto.FinancialYearsResponse;
import com.myledger.repository.ExpenseRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Financial-year logic: resolving a FY to a date range, labelling it, and listing the
 * FYs available for filtering. The FY start month is configurable (default April).
 */
@Service
@EnableConfigurationProperties(FinanceProperties.class)
public class FinanceService {

    // Widest sensible range, used when no FY filter is applied ("all time").
    private static final LocalDate MIN = LocalDate.of(1, 1, 1);
    private static final LocalDate MAX = LocalDate.of(9999, 1, 1);

    private final int startMonth;
    private final ExpenseRepository expenses;

    public FinanceService(FinanceProperties props, ExpenseRepository expenses) {
        this.startMonth = props.fyStartMonth();
        this.expenses = expenses;
    }

    /** Half-open date range [start, end) for a financial year. */
    public record DateRange(LocalDate start, LocalDate end) {
    }

    /** The FY start year that a given date falls into. */
    public int financialYearOf(LocalDate date) {
        return date.getMonthValue() >= startMonth ? date.getYear() : date.getYear() - 1;
    }

    public int currentFinancialYear() {
        return financialYearOf(LocalDate.now());
    }

    /** Range for a FY, or the all-time range when {@code fy} is null. */
    public DateRange range(Integer fy) {
        if (fy == null) {
            return new DateRange(MIN, MAX);
        }
        LocalDate start = LocalDate.of(fy, startMonth, 1);
        return new DateRange(start, start.plusYears(1));
    }

    public String label(int fy) {
        if (startMonth == 1) {
            return String.valueOf(fy);
        }
        int nextTwo = (fy + 1) % 100;
        return "%d-%02d".formatted(fy, nextTwo);
    }

    @Transactional(readOnly = true)
    public FinancialYearsResponse availableYears() {
        int current = currentFinancialYear();

        // Preserve descending order from the query, but guarantee the current FY is present.
        Set<Integer> ordered = new LinkedHashSet<>(expenses.distinctFinancialYears(startMonth));
        ordered.add(current);
        List<Integer> sorted = new ArrayList<>(ordered);
        sorted.sort((a, b) -> Integer.compare(b, a));

        List<FinancialYear> years = sorted.stream()
                .map(y -> new FinancialYear(y, label(y), y == current))
                .toList();

        return new FinancialYearsResponse(
                new FinancialYear(current, label(current), true),
                years);
    }
}
