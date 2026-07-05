package com.myledger.controller;

import com.myledger.dto.ProjectReport;
import com.myledger.security.AppPrincipal;
import com.myledger.security.Tabs;
import com.myledger.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Per-project reports. Owner: all projects. Contractor (with the Reports tab): only
 * their assigned projects, scoped to their own data (enforced in the service).
 */
@RestController
@RequestMapping("/api/reports")
public class ReportsController {

    private final ReportService reports;

    public ReportsController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/projects")
    public List<ProjectReport> projectReports(@RequestParam(name = "fy", required = false) Integer financialYear,
                                              @AuthenticationPrincipal AppPrincipal principal) {
        if (!principal.canView(Tabs.REPORTS)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not permitted");
        }
        return reports.projectReports(financialYear, principal);
    }
}
