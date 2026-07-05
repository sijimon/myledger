package com.myledger.service;

import com.myledger.dto.ProjectReport;
import com.myledger.dto.StatusCount;
import com.myledger.entity.Project;
import com.myledger.entity.Role;
import com.myledger.repository.ExpenseRepository;
import com.myledger.repository.FundRequestRepository;
import com.myledger.repository.ProjectRepository;
import com.myledger.security.AppPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

/**
 * Per-project reports. An owner sees every project with whole-project figures; a
 * contractor sees only their assigned projects, scoped to their own contributions.
 */
@Service
public class ReportService {

    private final ProjectRepository projects;
    private final ExpenseRepository expenses;
    private final FundRequestRepository fundRequests;
    private final FinanceService finance;
    private final ProjectMemberService members;

    public ReportService(ProjectRepository projects, ExpenseRepository expenses,
                         FundRequestRepository fundRequests, FinanceService finance,
                         ProjectMemberService members) {
        this.projects = projects;
        this.expenses = expenses;
        this.fundRequests = fundRequests;
        this.finance = finance;
        this.members = members;
    }

    @Transactional(readOnly = true)
    public List<ProjectReport> projectReports(Integer fy, AppPrincipal caller) {
        FinanceService.DateRange r = finance.range(fy);
        LocalDate start = r.start();
        LocalDate end = r.end();
        OffsetDateTime startDt = start.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endDt = end.atStartOfDay().atOffset(ZoneOffset.UTC);

        boolean owner = caller.role() == Role.ROLE_OWNER;
        Long who = owner ? null : caller.userId();
        Set<Long> assigned = owner ? Set.of() : Set.copyOf(members.assignedProjectIds(caller.userId()));

        return projects.findAllByOrderByIdAsc().stream()
                .filter(p -> owner || assigned.contains(p.getId()))
                .map(p -> buildReport(p, start, end, startDt, endDt, who))
                .toList();
    }

    private ProjectReport buildReport(Project p, LocalDate start, LocalDate end,
                                      OffsetDateTime startDt, OffsetDateTime endDt, Long who) {
        Long pid = p.getId();
        List<StatusCount> byStatus = fundRequests.reportByStatus(pid, who, startDt, endDt);
        long fundCount = byStatus.stream().mapToLong(StatusCount::count).sum();
        BigDecimal fundTotal = byStatus.stream()
                .map(StatusCount::total).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ProjectReport(
                pid,
                p.getName(),
                p.getCurrency(),
                expenses.reportTotal(pid, start, end, who),
                expenses.reportCount(pid, start, end, who),
                expenses.reportByCategory(pid, start, end, who),
                expenses.reportByPhase(pid, start, end, who),
                fundTotal,
                fundCount,
                byStatus
        );
    }
}
