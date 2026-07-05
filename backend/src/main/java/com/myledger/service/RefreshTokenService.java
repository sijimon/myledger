package com.myledger.service;

import com.myledger.config.SecurityProperties;
import com.myledger.entity.RefreshToken;
import com.myledger.entity.User;
import com.myledger.repository.RefreshTokenRepository;
import com.myledger.security.TokenHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

/**
 * Issues, validates, rotates, and revokes opaque refresh tokens. Only the SHA-256
 * hash is persisted; the raw value is returned once (to be set as an httpOnly cookie).
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final RefreshTokenRepository repository;
    private final long ttlDays;

    public RefreshTokenService(RefreshTokenRepository repository, SecurityProperties props) {
        this.repository = repository;
        this.ttlDays = props.refreshTokenTtlDays();
    }

    /** The raw token value plus its expiry; the raw value is never persisted. */
    public record IssuedToken(String rawValue, OffsetDateTime expiresAt) {
    }

    @Transactional
    public IssuedToken issue(User user) {
        String raw = generateRawToken();
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(ttlDays, ChronoUnit.DAYS);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(TokenHasher.sha256Hex(raw));
        token.setExpiresAt(expiresAt);
        repository.save(token);

        return new IssuedToken(raw, expiresAt);
    }

    /**
     * Verifies a raw refresh token and, if valid, rotates it: the presented token is
     * revoked and a fresh one issued. Returns empty if the token is unknown, expired,
     * or already revoked.
     */
    @Transactional
    public Optional<Rotation> rotate(String rawValue) {
        return findActive(rawValue).map(existing -> {
            existing.setRevokedAt(OffsetDateTime.now());
            IssuedToken next = issue(existing.getUser());
            return new Rotation(existing.getUser(), next);
        });
    }

    public record Rotation(User user, IssuedToken next) {
    }

    @Transactional
    public void revoke(String rawValue) {
        findActive(rawValue).ifPresent(t -> t.setRevokedAt(OffsetDateTime.now()));
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllForUser(userId, OffsetDateTime.now());
    }

    private Optional<RefreshToken> findActive(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        String hash = TokenHasher.sha256Hex(rawValue);
        return repository.findByTokenHash(hash)
                .filter(t -> t.isActive(OffsetDateTime.now()));
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }
}
