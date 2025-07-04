package com.study.demo.testweatherapi.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Configuration
public class WebClientConfig {

    @Value("${kma.base-url}")
    private String kmaBaseUrl;

    @Value("${kma.auth-key}")
    private String authKey;

    @Bean
    public WebClient kmaWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(kmaBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultUriVariables(Map.of("authKey", authKey))
                .build();
    }
}
