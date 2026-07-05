package com.myledger.controller;

import com.myledger.dto.ExpenseRequest;
import com.myledger.dto.ExpenseResponse;
import com.myledger.security.AppPrincipal;
import com.myledger.security.Tabs;
import com.myledger.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Expenses. Owner: full access. Contractor with the Expenses tab: their own expenses,
 * scoped to assigned projects (enforced in the service).
 */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    private static void requireExpensesTab(AppPrincipal principal) {
        if (!principal.canView(Tabs.EXPENSES)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not permitted");
        }
    }

    @GetMapping
    public List<ExpenseResponse> list(@RequestParam(name = "fy", required = false) Integer financialYear,
                                      @AuthenticationPrincipal AppPrincipal principal) {
        requireExpensesTab(principal);
        return service.list(financialYear, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@Valid @RequestBody ExpenseRequest request,
                                  @AuthenticationPrincipal AppPrincipal principal) {
        requireExpensesTab(principal);
        return service.create(request, principal);
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request,
                                  @AuthenticationPrincipal AppPrincipal principal) {
        requireExpensesTab(principal);
        return service.update(id, request, principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AppPrincipal principal) {
        requireExpensesTab(principal);
        service.delete(id, principal);
    }
}
