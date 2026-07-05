package com.myledger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds {@code myledger.cors.*}. In production the SPA is served same-origin,
 * so this list is typically empty.
 */
@ConfigurationProperties(prefix = "myledger.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
