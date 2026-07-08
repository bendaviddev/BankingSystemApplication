package com.benbanking.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Value("${app.cors-origins:http://localhost:5173}")
    private String corsOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        String[] origins = corsOrigins.split(",");
        for (int i = 0; i < origins.length; i++) {
            origins[i] = origins[i].trim();
        }

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(origins)
                        .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        // Bearer tokens travel in the Authorization header, not cookies — no
                        // credentialed CORS requests are needed (invariant #13).
                        .allowCredentials(false);
            }
        };
    }
}
