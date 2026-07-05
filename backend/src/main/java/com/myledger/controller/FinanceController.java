package com.myledger.controller;

import com.myledger.dto.FinancialYearsResponse;
import com.myledger.service.FinanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Owner-only finance metadata (financial years available for filtering). */
@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService finance;

    public FinanceController(FinanceService finance) {
        this.finance = finance;
    }

    @GetMapping("/years")
    public FinancialYearsResponse years() {
        return finance.availableYears();
    }
}
