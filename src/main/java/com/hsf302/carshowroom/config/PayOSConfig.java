package com.hsf302.carshowroom.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
@EnableConfigurationProperties({PayOSProperties.class, PayOSPayoutProperties.class})
public class PayOSConfig {

    @Bean
    public PayOS payOS(PayOSProperties properties) {
        return new PayOS(properties.clientId(), properties.apiKey(), properties.checksumKey());
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
