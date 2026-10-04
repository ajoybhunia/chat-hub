package com.chathub.config;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Application-wide settings bound from configuration. Fail-fast: startup aborts
 * when a required environment variable (e.g. JWT_SECRET) is missing.
 *
 * @param websocketAllowedOriginPatterns browser origins permitted to complete
 *                                       the WebSocket handshake (may be {@code null}
 *                                       when unset — callers fall back to
 *                                       same-origin-only)
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@NotBlank String jwtSecret, List<String> websocketAllowedOriginPatterns) {
}
