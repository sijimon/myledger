package com.myledger.service;

import com.myledger.dto.ProjectRequest;
import com.myledger.dto.ProjectResponse;
import com.myledger.entity.Project;
import com.myledger.repository.ExpenseRepository;
import com.myledger.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class ProjectService {

    private static final Set<String> STATUSES = Set.of("ACTIVE", "ARCHIVED");

    private final ProjectRepository projects;
    private final ExpenseRepository expenses;

    public ProjectService(ProjectRepository projects, ExpenseRepository expenses) {
        this.projects = projects;
        this.expenses = expenses;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return projects.findAllByOrderByIdAsc().stream().map(ProjectResponse::from).toList();
    }

    @Transactional
    public ProjectResponse create(ProjectRequest req) {
        String name = req.name().trim();
        if (projects.existsByNameIgnoreCase(name)) {
            throw conflict("A project with that name already exists");
        }
        Project p = new Project();
        p.setName(name);
        p.setDescription(req.description());
        p.setStartDate(req.startDate());
        p.setStatus(resolveStatus(req.status(), "ACTIVE"));
        p.setCurrency(resolveCurrency(req.currency(), "USD"));
        return ProjectResponse.from(projects.save(p));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest req) {
        Project p = projects.findById(id).orElseThrow(() -> notFound());
        String name = req.name().trim();
        if (projects.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw conflict("A project with that name already exists");
        }
        p.setName(name);
        p.setDescription(req.description());
        p.setStartDate(req.startDate());
        p.setStatus(resolveStatus(req.status(), p.getStatus()));
        p.setCurrency(resolveCurrency(req.currency(), p.getCurrency()));
        return ProjectResponse.from(projects.save(p));
    }

    @Transactional
    public void delete(Long id) {
        if (!projects.existsById(id)) {
            throw notFound();
        }
        if (expenses.existsByProjectId(id)) {
            throw conflict("Project has expenses — archive it instead of deleting");
        }
        projects.deleteById(id);
    }

    private String resolveCurrency(String currency, String fallback) {
        if (currency == null || currency.isBlank()) {
            return fallback;
        }
        return currency.trim().toUpperCase();
    }

    private String resolveStatus(String status, String fallback) {
        if (status == null || status.isBlank()) {
            return fallback;
        }
        String upper = status.toUpperCase();
        if (!STATUSES.contains(upper)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
        return upper;
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
    }

    private static ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }
}
