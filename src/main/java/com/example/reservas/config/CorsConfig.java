package com.example.reservas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final Environment environment;
    private final String allowedOrigins;
    private final boolean allowCredentials;

    public CorsConfig(Environment environment,
                      @Value("${APP_CORS_ALLOWED_ORIGINS:}") String allowedOrigins,
                      @Value("${APP_CORS_ALLOW_CREDENTIALS:true}") boolean allowCredentials) {
        this.environment = environment;
        this.allowedOrigins = allowedOrigins;
        this.allowCredentials = allowCredentials;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = parseOrigins(allowedOrigins);
        if (origins.length == 0) {
            return;
        }

        var mapping = registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept")
                .allowCredentials(allowCredentials)
                .maxAge(3600);

        if (isProdProfile()) {
            mapping.allowedOrigins(origins);
        } else {
            mapping.allowedOriginPatterns(origins);
        }
    }

    static String[] parseOrigins(String allowedOrigins) {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);
    }

    private boolean isProdProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
}
