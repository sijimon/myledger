package com.myledger.dto;

import java.util.List;

/**
 * Partial update for a user. {@code roleId} assigns a custom role (sets tabs +
 * contractor class). {@code role} switches the account type (e.g. to ROLE_OWNER).
 * Null fields are left unchanged.
 */
public record UpdateUserRequest(String role, Boolean enabled, List<String> tabs, Long roleId) {
}
