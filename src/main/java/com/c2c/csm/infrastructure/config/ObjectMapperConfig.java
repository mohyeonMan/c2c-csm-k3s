package com.c2c.csm.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class ObjectMapperConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder().build();
    }
    
}
