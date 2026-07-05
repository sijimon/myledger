package com.myledger.dto;

import java.util.List;

public record MeResponse(Long userId, String email, String role, List<String> tabs) {
}
