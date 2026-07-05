package com.myledger.dto;

import com.myledger.entity.Category;

public record CategoryResponse(Long id, Long projectId, String name, boolean taxRelevant, boolean active) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getProject().getId(), c.getName(), c.isTaxRelevant(), c.isActive());
    }
}
