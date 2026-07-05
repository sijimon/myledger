package com.myledger.service;

import com.myledger.dto.ExpenseRequest;
import com.myledger.dto.ExpenseResponse;
import com.myledger.entity.Category;
import com.myledger.entity.Expense;
import com.myledger.entity.Phase;
import com.myledger.entity.Project;
import com.myledger.entity.Role;
import com.myledger.entity.StoredFile;
import com.myledger.repository.CategoryRepository;
import com.myledger.repository.ExpenseRepository;
import com.myledger.repository.PhaseRepository;
import com.myledger.repository.ProjectRepository;
import com.myledger.repository.StoredFileRepository;
import com.myledger.security.AppPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Expenses. Owners manage all expenses. A contractor (with the Expenses tab) may
 * add/edit/delete their OWN expenses, and only for projects they are assigned to.
 */
@Service
public class ExpenseService {

    private final ExpenseRepository expenses;
    private final CategoryRepository categories;
    private final ProjectRepository projects;
    private final PhaseRepository phases;
    private final StoredFileRepository files;
    private final FinanceService finance;
    private final ProjectMemberService members;

    public ExpenseService(ExpenseRepository expenses,
                          CategoryRepository categories,
                          ProjectRepository projects,
                          PhaseRepository phases,
                          StoredFileRepository files,
                          FinanceService finance,
                          ProjectMemberService members) {
        this.expenses = expenses;
        this.categories = categories;
        this.projects = projects;
        this.phases = phases;
        this.files = files;
        this.finance = finance;
        this.members = members;
    }

    /** Owner sees all; a contractor sees only their own expenses. */
    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(Integer fy, AppPrincipal caller) {
        FinanceService.DateRange r = finance.range(fy);
        List<Expense> found = isOwner(caller)
                ? expenses.findForListing(r.start(), r.end())
                : expenses.findForListingByCreator(r.start(), r.end(), caller.userId());
        return found.stream().map(ExpenseResponse::from).toList();
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest req, AppPrincipal caller) {
        if (!isOwner(caller)) {
            requireAssigned(caller.userId(), req.projectId());
        }
        Expense e = new Expense();
        apply(e, req, isOwner(caller));
        e.setCreatedBy(caller.userId());
        return ExpenseResponse.from(expenses.save(e));
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest req, AppPrincipal caller) {
        Expense e = expenses.findByIdWithRelations(id).orElseThrow(() -> notFound("Expense"));
        if (!isOwner(caller)) {
            requireOwnExpense(e, caller.userId());
            requireAssigned(caller.userId(), req.projectId());
        }
        apply(e, req, isOwner(caller));
        return ExpenseResponse.from(expenses.save(e));
    }

    @Transactional
    public void delete(Long id, AppPrincipal caller) {
        Expense e = expenses.findById(id).orElseThrow(() -> notFound("Expense"));
        if (!isOwner(caller)) {
            requireOwnExpense(e, caller.userId());
        }
        expenses.delete(e);
    }

    /** Applies request fields, validating referenced ids. Receipts are owner-only. */
    private void apply(Expense e, ExpenseRequest req, boolean owner) {
        Category category = categories.findById(req.categoryId())
                .orElseThrow(() -> badRequest("Unknown category"));
        Project project = projects.findById(req.projectId())
                .orElseThrow(() -> badRequest("Unknown project"));
        if (!category.getProject().getId().equals(project.getId())) {
            throw badRequest("Category does not belong to the selected project");
        }

        e.setAmount(req.amount());
        e.setExpenseDate(req.expenseDate());
        e.setCategory(category);
        e.setProject(project);
        e.setVendor(req.vendor());
        e.setNotes(req.notes());

        if (req.phaseId() == null) {
            e.setPhase(null);
        } else {
            Phase phase = phases.findById(req.phaseId())
                    .orElseThrow(() -> badRequest("Unknown phase"));
            if (!phase.getProject().getId().equals(project.getId())) {
                throw badRequest("Phase does not belong to the selected project");
            }
            e.setPhase(phase);
        }

        // Only owners may attach receipts (file storage is owner-scoped).
        if (owner && req.fileId() != null) {
            StoredFile receipt = files.findById(req.fileId())
                    .orElseThrow(() -> badRequest("Unknown file"));
            e.setReceipt(receipt);
        } else if (owner) {
            e.setReceipt(null);
        }
    }

    private boolean isOwner(AppPrincipal caller) {
        return caller.role() == Role.ROLE_OWNER;
    }

    private void requireAssigned(Long userId, Long projectId) {
        if (!members.isAssigned(userId, projectId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not assigned to this project");
        }
    }

    private void requireOwnExpense(Expense e, Long userId) {
        if (!userId.equals(e.getCreatedBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your expense");
        }
    }

    private static ResponseStatusException notFound(String what) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, what + " not found");
    }

    private static ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
