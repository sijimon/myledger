package com.myledger.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Create a user. Provide {@code roleId} to assign a custom role (makes the user a
 * contractor-class account and sets their tabs from the role). Otherwise {@code role}
 * (ROLE_OWNER/ROLE_CONTRACTOR) + optional {@code tabs} are used.
 */
public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        String role,
        List<String> tabs,
        Long roleId
) {
}
