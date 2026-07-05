package com.myledger.dto;

import com.myledger.entity.Phase;

public record PhaseResponse(Long id, Long projectId, String name, boolean active) {
    public static PhaseResponse from(Phase p) {
        return new PhaseResponse(p.getId(), p.getProject().getId(), p.getName(), p.isActive());
    }
}
