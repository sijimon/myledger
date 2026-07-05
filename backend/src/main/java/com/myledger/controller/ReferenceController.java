package com.myledger.controller;

import com.myledger.dto.CategoryResponse;
import com.myledger.dto.PhaseResponse;
import com.myledger.dto.ProjectOption;
import com.myledger.entity.Role;
import com.myledger.repository.CategoryRepository;
import com.myledger.repository.PhaseRepository;
import com.myledger.repository.ProjectRepository;
import com.myledger.security.AppPrincipal;
import com.myledger.service.ProjectMemberService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Reference data for building expense forms, available to any authenticated user.
 * For an owner: all active projects and their categories/phases. For a contractor:
 * only projects they are assigned to (and those projects' categories/phases).
 */
@RestController
@RequestMapping("/api/reference")
public class ReferenceController {

    private final ProjectRepository projects;
    private final CategoryRepository categories;
    private final PhaseRepository phases;
    private final ProjectMemberService members;

    public ReferenceController(ProjectRepository projects, CategoryRepository categories,
                              PhaseRepository phases, ProjectMemberService members) {
        this.projects = projects;
        this.categories = categories;
        this.phases = phases;
        this.members = members;
    }

    @GetMapping("/projects")
    public List<ProjectOption> activeProjects(@AuthenticationPrincipal AppPrincipal principal) {
        boolean owner = principal.role() == Role.ROLE_OWNER;
        List<Long> assigned = owner ? null : members.assignedProjectIds(principal.userId());
        return projects.findAllByOrderByIdAsc().stream()
                .filter(p -> "ACTIVE".equals(p.getStatus()))
                .filter(p -> owner || assigned.contains(p.getId()))
                .map(ProjectOption::from)
                .toList();
    }

    @GetMapping("/projects/{projectId}/categories")
    public List<CategoryResponse> categories(@PathVariable Long projectId,
                                             @AuthenticationPrincipal AppPrincipal principal) {
        requireProjectAccess(projectId, principal);
        return categories.findByProjectIdOrderByNameAsc(projectId).stream()
                .filter(c -> c.isActive())
                .map(CategoryResponse::from)
                .toList();
    }

    @GetMapping("/projects/{projectId}/phases")
    public List<PhaseResponse> phases(@PathVariable Long projectId,
                                      @AuthenticationPrincipal AppPrincipal principal) {
        requireProjectAccess(projectId, principal);
        return phases.findByProjectIdOrderByIdAsc(projectId).stream()
                .filter(p -> p.isActive())
                .map(PhaseResponse::from)
                .toList();
    }

    private void requireProjectAccess(Long projectId, AppPrincipal principal) {
        if (principal.role() == Role.ROLE_OWNER) {
            return;
        }
        if (!members.isAssigned(principal.userId(), projectId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not assigned to this project");
        }
    }
}
