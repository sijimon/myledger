package com.myledger.dto;

import com.myledger.entity.RoleTemplate;
import com.myledger.security.Tabs;

import java.util.List;

public record RoleResponse(Long id, String name, List<String> tabs, boolean builtIn) {
    public static RoleResponse from(RoleTemplate r) {
        return new RoleResponse(r.getId(), r.getName(), List.copyOf(Tabs.parse(r.getTabs())), r.isBuiltIn());
    }
}
