package com.kinyozi.royale.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // 1. Properly instantiate the ObjectMapper instance first
        ObjectMapper mapper = new ObjectMapper();

        // 2. Register the module to handle modern Java 8 Date/Time types correctly
        mapper.registerModule(new JavaTimeModule());

        // 3. Disable numeric timestamps to output clean ISO-8601 string dates (e.g., "2026-06-02")
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 4. Return the fully configured mapper instance
        return mapper;
    }
}