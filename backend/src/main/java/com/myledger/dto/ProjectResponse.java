package com.myledger.dto;

import com.myledger.entity.Project;

import java.time.LocalDate;

public record ProjectResponse(Long id, String name, String description, LocalDate startDate, String status, String currency) {
    public static ProjectResponse from(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getStartDate(), p.getStatus(), p.getCurrency());
    }
}
