package com.myledger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code myledger.security.*} configuration block.
 */
@ConfigurationProperties(prefix = "myledger.security")
public record SecurityProperties(
        String jwtSecret,
        long accessTokenTtlMinutes,
        long refreshTokenTtlDays,
        boolean cookieSecure
) {
}
