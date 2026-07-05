package com.myledger.dto;

/**
 * Returned on login/refresh. The refresh token is NOT in the body — it is set as an
 * httpOnly cookie by the controller.
 */
public record TokenResponse(
        String accessToken,
        long expiresInSeconds,
        String email,
        String role
) {
}
