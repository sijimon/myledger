package com.myledger.service;

import com.myledger.dto.DashboardSummary;
import com.myledger.dto.NameAmount;
import com.myledger.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    private final ExpenseRepository expenses;
    private final FinanceService finance;

    public DashboardService(ExpenseRepository expenses, FinanceService finance) {
        this.expenses = expenses;
        this.finance = finance;
    }

    /** Summary scoped to a financial year, or all time when {@code fy} is null. */
    @Transactional(readOnly = true)
    public DashboardSummary summary(Integer fy) {
        FinanceService.DateRange r = finance.range(fy);
        LocalDate start = r.start();
        LocalDate end = r.end();

        List<NameAmount> byMonth = expenses.totalByMonth(start, end).stream()
                .map(row -> new NameAmount(row.getMonth(), row.getTotal()))
                .toList();

        String scopeLabel = fy == null ? "All time" : finance.label(fy);

        return new DashboardSummary(
                fy,
                scopeLabel,
                expenses.totalSpend(start, end),
                expenses.countInRange(start, end),
                expenses.totalByCategory(start, end),
                expenses.totalByProject(start, end),
                expenses.totalByPhase(start, end),
                byMonth
        );
    }
}
