package com.myledger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code myledger.bootstrap.*} — the one-time owner seed credentials.
 */
@ConfigurationProperties(prefix = "myledger.bootstrap")
public record BootstrapProperties(String ownerEmail, String ownerPassword) {
}
