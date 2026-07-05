package com.myledger.dto;

import java.util.List;

/**
 * Partial update for a user's role, enabled status, and/or granted tabs.
 * Null fields are left unchanged.
 */
public record UpdateUserRequest(String role, Boolean enabled, List<String> tabs) {
}
