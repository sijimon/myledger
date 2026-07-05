package com.myledger.dto;

import com.myledger.entity.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        BigDecimal amount,
        LocalDate expenseDate,
        Long categoryId,
        String categoryName,
        Long projectId,
        String projectName,
        String projectCurrency,
        Long phaseId,
        String phaseName,
        String vendor,
        String notes,
        Long fileId,
        String receiptName
) {
    public static ExpenseResponse from(Expense e) {
        return new ExpenseResponse(
                e.getId(),
                e.getAmount(),
                e.getExpenseDate(),
                e.getCategory().getId(),
                e.getCategory().getName(),
                e.getProject().getId(),
                e.getProject().getName(),
                e.getProject().getCurrency(),
                e.getPhase() == null ? null : e.getPhase().getId(),
                e.getPhase() == null ? null : e.getPhase().getName(),
                e.getVendor(),
                e.getNotes(),
                e.getReceipt() == null ? null : e.getReceipt().getId(),
                e.getReceipt() == null ? null : e.getReceipt().getOriginalName()
        );
    }
}
