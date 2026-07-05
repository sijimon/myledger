package com.myledger.controller;

import com.myledger.config.SecurityProperties;
import com.myledger.dto.LoginRequest;
import com.myledger.dto.TokenResponse;
import com.myledger.security.LoginRateLimiter;
import com.myledger.service.AuthService;
import com.myledger.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final LoginRateLimiter rateLimiter;
    private final boolean cookieSecure;

    public AuthController(AuthService authService,
                         LoginRateLimiter rateLimiter,
                         SecurityProperties props) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
        this.cookieSecure = props.cookieSecure();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        if (!rateLimiter.tryConsume(httpRequest.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts");
        }
        AuthService.AuthResult result = authService.login(request.email(), request.password());
        return respondWithTokens(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
        }
        AuthService.AuthResult result = authService.refresh(refreshToken);
        return respondWithTokens(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie().toString())
                .build();
    }

    private ResponseEntity<TokenResponse> respondWithTokens(AuthService.AuthResult result) {
        RefreshTokenService.IssuedToken refresh = result.refresh();
        long maxAge = Math.max(0, Duration.between(OffsetDateTime.now(), refresh.expiresAt()).getSeconds());
        ResponseCookie cookie = baseCookie(refresh.rawValue())
                .maxAge(maxAge)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.body());
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path(COOKIE_PATH);
    }

    private ResponseCookie expiredCookie() {
        return baseCookie("").maxAge(0).build();
    }
}
