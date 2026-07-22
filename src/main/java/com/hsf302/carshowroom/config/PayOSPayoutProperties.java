package com.hsf302.carshowroom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payos.payout")
public record PayOSPayoutProperties(
        String clientId,
        String apiKey,
        String checksumKey,
        String baseUrl
) {
    public boolean hasCredentials() {
        return hasText(clientId) && hasText(apiKey) && hasText(checksumKey);
    }

    public String resolvedBaseUrl() {
        return hasText(baseUrl) ? baseUrl.replaceAll("/+$", "") : "https://api-merchant.payos.vn";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
