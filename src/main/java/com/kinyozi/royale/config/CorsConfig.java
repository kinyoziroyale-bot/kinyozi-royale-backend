package com.kinyozi.royale.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Single, application-wide CORS configuration.
 *
 * Origins are read from the {@code royale.app.cors} property
 * (env {@code CORS_ORIGINS}), comma separated. Wildcards ("*") are
 * rejected — every allowed origin must be listed explicitly.
 */
@Configuration
public class CorsConfig {

    @Value("${royale.app.cors:http://localhost:5173,http://localhost:8080,http://localhost:8082}")
    private String corsOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(corsOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> !s.equals("*"))
                .collect(Collectors.toList());

        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(origins);
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "Origin",
                "X-Requested-With", "X-Tenant-Id", "X-Supabase-Access-Token")); // <-- ADDED HERE
        c.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        c.setAllowCredentials(true);
        c.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", c);
        return src;
    }
}