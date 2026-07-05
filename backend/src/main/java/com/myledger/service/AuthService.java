package com.myledger.service;

import com.myledger.config.SecurityProperties;
import com.myledger.dto.TokenResponse;
import com.myledger.entity.User;
import com.myledger.repository.UserRepository;
import com.myledger.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Password authentication and token minting. Credential failures are deliberately
 * indistinguishable (same 401, no "user not found" vs "wrong password" leak).
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final long accessTtlSeconds;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       SecurityProperties props) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.accessTtlSeconds = props.accessTokenTtlMinutes() * 60;
    }

    /** Access-token body plus the freshly issued refresh token (for the cookie). */
    public record AuthResult(TokenResponse body, RefreshTokenService.IssuedToken refresh) {
    }

    @Transactional
    public AuthResult login(String email, String rawPassword) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(User::isEnabled)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        return buildResult(user);
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        // Transactional so the lazily-loaded User on the rotated token stays attached
        // while the access token is built.
        return refreshTokenService.rotate(rawRefreshToken)
                .map(rotation -> buildResult(rotation.user(), rotation.next()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthResult buildResult(User user) {
        return buildResult(user, refreshTokenService.issue(user));
    }

    private AuthResult buildResult(User user, RefreshTokenService.IssuedToken refresh) {
        String accessToken = jwtService.issueAccessToken(user);
        TokenResponse body = new TokenResponse(
                accessToken, accessTtlSeconds, user.getEmail(), user.getRole().name());
        return new AuthResult(body, refresh);
    }
}
