package com.myledger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code myledger.storage.*}. Files are stored on local disk (Phase 1),
 * outside any web root, and served only through an authenticated endpoint.
 */
@ConfigurationProperties(prefix = "myledger.storage")
public record StorageProperties(String dir) {
}
