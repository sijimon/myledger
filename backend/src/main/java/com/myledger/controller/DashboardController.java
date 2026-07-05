package com.myledger.controller;

import com.myledger.dto.DashboardSummary;
import com.myledger.security.AppPrincipal;
import com.myledger.security.Tabs;
import com.myledger.service.DashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/summary")
    public DashboardSummary summary(@RequestParam(name = "fy", required = false) Integer financialYear,
                                    @AuthenticationPrincipal AppPrincipal principal) {
        // Owner, or a contractor granted the Dashboard tab.
        if (!principal.canView(Tabs.DASHBOARD)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not permitted");
        }
        return dashboard.summary(financialYear);
    }
}
