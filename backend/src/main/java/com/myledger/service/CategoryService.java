package com.myledger.service;

import com.myledger.dto.CategoryRequest;
import com.myledger.dto.CategoryResponse;
import com.myledger.entity.Category;
import com.myledger.entity.Project;
import com.myledger.repository.CategoryRepository;
import com.myledger.repository.ExpenseRepository;
import com.myledger.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categories;
    private final ProjectRepository projects;
    private final ExpenseRepository expenses;

    public CategoryService(CategoryRepository categories, ProjectRepository projects, ExpenseRepository expenses) {
        this.categories = categories;
        this.projects = projects;
        this.expenses = expenses;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listForProject(Long projectId) {
        requireProject(projectId);
        return categories.findByProjectIdOrderByNameAsc(projectId).stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(Long projectId, CategoryRequest req) {
        Project project = requireProject(projectId);
        String name = req.name().trim();
        if (categories.existsByProjectIdAndNameIgnoreCase(projectId, name)) {
            throw conflict("A category with that name already exists in this project");
        }
        Category c = new Category();
        c.setProject(project);
        c.setName(name);
        c.setTaxRelevant(Boolean.TRUE.equals(req.taxRelevant()));
        c.setActive(req.active() == null || req.active());
        return CategoryResponse.from(categories.save(c));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest req) {
        Category c = categories.findById(id).orElseThrow(CategoryService::notFound);
        String name = req.name().trim();
        if (categories.existsByProjectIdAndNameIgnoreCaseAndIdNot(c.getProject().getId(), name, id)) {
            throw conflict("A category with that name already exists in this project");
        }
        c.setName(name);
        c.setTaxRelevant(Boolean.TRUE.equals(req.taxRelevant()));
        if (req.active() != null) {
            c.setActive(req.active());
        }
        return CategoryResponse.from(categories.save(c));
    }

    @Transactional
    public void delete(Long id) {
        if (!categories.existsById(id)) {
            throw notFound();
        }
        if (expenses.existsByCategoryId(id)) {
            throw conflict("Category has expenses — archive it instead of deleting");
        }
        categories.deleteById(id);
    }

    private Project requireProject(Long projectId) {
        return projects.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
    }

    private static ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }
}
