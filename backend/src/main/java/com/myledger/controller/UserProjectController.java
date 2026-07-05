package com.myledger.controller;

import com.myledger.service.ProjectMemberService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Owner-only management of a contractor's project assignments (under /api/users → owner-only). */
@RestController
@RequestMapping("/api/users/{id}/projects")
public class UserProjectController {

    private final ProjectMemberService service;

    public UserProjectController(ProjectMemberService service) {
        this.service = service;
    }

    @GetMapping
    public List<Long> get(@PathVariable Long id) {
        return service.assignedProjectIds(id);
    }

    @PutMapping
    public List<Long> set(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        return service.setAssignments(id, body.get("projectIds"));
    }
}
