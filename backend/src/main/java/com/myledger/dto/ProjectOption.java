package com.myledger.dto;

import com.myledger.entity.Project;

/** Minimal project reference for pickers (e.g. the fund-request form). */
public record ProjectOption(Long id, String name, String currency) {
    public static ProjectOption from(Project p) {
        return new ProjectOption(p.getId(), p.getName(), p.getCurrency());
    }
}
