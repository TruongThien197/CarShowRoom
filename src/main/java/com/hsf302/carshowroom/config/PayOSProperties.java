package com.hsf302.carshowroom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payos")
public record PayOSProperties(
        String clientId,
        String apiKey,
        String checksumKey,
        String returnUrl,
        String cancelUrl,
        String webhookUrl
) {
    public boolean hasCredentials() {
        return hasText(clientId) && hasText(apiKey) && hasText(checksumKey);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
