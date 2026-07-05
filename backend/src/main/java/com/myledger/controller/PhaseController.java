package com.myledger.controller;

import com.myledger.dto.PhaseRequest;
import com.myledger.dto.PhaseResponse;
import com.myledger.service.PhaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Owner-only phase management. Phases are nested under a project for listing/creation;
 * update/delete address a phase directly by id. Access enforced in SecurityConfig
 * ({@code /api/projects/**} and {@code /api/phases/**}).
 */
@RestController
public class PhaseController {

    private final PhaseService service;

    public PhaseController(PhaseService service) {
        this.service = service;
    }

    @GetMapping("/api/projects/{projectId}/phases")
    public List<PhaseResponse> list(@PathVariable Long projectId) {
        return service.listForProject(projectId);
    }

    @PostMapping("/api/projects/{projectId}/phases")
    @ResponseStatus(HttpStatus.CREATED)
    public PhaseResponse create(@PathVariable Long projectId, @Valid @RequestBody PhaseRequest request) {
        return service.create(projectId, request);
    }

    @PutMapping("/api/phases/{id}")
    public PhaseResponse update(@PathVariable Long id, @Valid @RequestBody PhaseRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/api/phases/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
