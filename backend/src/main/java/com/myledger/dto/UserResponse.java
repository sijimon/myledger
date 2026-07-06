package com.myledger.dto;

import com.myledger.entity.User;
import com.myledger.security.Tabs;

import java.time.OffsetDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String role,
        boolean enabled,
        List<String> tabs,
        Long roleId,
        String roleName,
        OffsetDateTime createdAt
) {
    public static UserResponse from(User u, String roleName) {
        return new UserResponse(u.getId(), u.getEmail(), u.getRole().name(), u.isEnabled(),
                List.copyOf(Tabs.parse(u.getTabs())), u.getRoleId(), roleName, u.getCreatedAt());
    }
}
