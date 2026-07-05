package com.myledger.controller;

import com.myledger.dto.CategoryRequest;
import com.myledger.dto.CategoryResponse;
import com.myledger.service.CategoryService;
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
 * Owner-only category management, scoped per project. Listing/creation are nested under
 * a project; update/delete address a category directly by id. Access enforced in
 * SecurityConfig ({@code /api/projects/**} and {@code /api/categories/**}).
 */
@RestController
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping("/api/projects/{projectId}/categories")
    public List<CategoryResponse> list(@PathVariable Long projectId) {
        return service.listForProject(projectId);
    }

    @PostMapping("/api/projects/{projectId}/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@PathVariable Long projectId, @Valid @RequestBody CategoryRequest request) {
        return service.create(projectId, request);
    }

    @PutMapping("/api/categories/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/api/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
