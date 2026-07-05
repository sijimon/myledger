package com.myledger.service;

import com.myledger.dto.PhaseRequest;
import com.myledger.dto.PhaseResponse;
import com.myledger.entity.Phase;
import com.myledger.entity.Project;
import com.myledger.repository.ExpenseRepository;
import com.myledger.repository.PhaseRepository;
import com.myledger.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PhaseService {

    private final PhaseRepository phases;
    private final ProjectRepository projects;
    private final ExpenseRepository expenses;

    public PhaseService(PhaseRepository phases, ProjectRepository projects, ExpenseRepository expenses) {
        this.phases = phases;
        this.projects = projects;
        this.expenses = expenses;
    }

    @Transactional(readOnly = true)
    public List<PhaseResponse> listForProject(Long projectId) {
        requireProject(projectId);
        return phases.findByProjectIdOrderByIdAsc(projectId).stream().map(PhaseResponse::from).toList();
    }

    @Transactional
    public PhaseResponse create(Long projectId, PhaseRequest req) {
        Project project = requireProject(projectId);
        String name = req.name().trim();
        if (phases.existsByProjectIdAndNameIgnoreCase(projectId, name)) {
            throw conflict("A phase with that name already exists in this project");
        }
        Phase p = new Phase();
        p.setProject(project);
        p.setName(name);
        p.setActive(req.active() == null || req.active());
        return PhaseResponse.from(phases.save(p));
    }

    @Transactional
    public PhaseResponse update(Long id, PhaseRequest req) {
        Phase p = phases.findById(id).orElseThrow(PhaseService::notFound);
        String name = req.name().trim();
        if (phases.existsByProjectIdAndNameIgnoreCaseAndIdNot(p.getProject().getId(), name, id)) {
            throw conflict("A phase with that name already exists in this project");
        }
        p.setName(name);
        if (req.active() != null) {
            p.setActive(req.active());
        }
        return PhaseResponse.from(phases.save(p));
    }

    @Transactional
    public void delete(Long id) {
        if (!phases.existsById(id)) {
            throw notFound();
        }
        if (expenses.existsByPhaseId(id)) {
            throw conflict("Phase has expenses — archive it instead of deleting");
        }
        phases.deleteById(id);
    }

    private Project requireProject(Long projectId) {
        return projects.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Phase not found");
    }

    private static ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }
}
