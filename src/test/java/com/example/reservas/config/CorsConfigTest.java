package com.example.reservas.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void parseOriginsReturnsEmptyArrayForBlankValue() {
        assertThat(CorsConfig.parseOrigins("")).isEmpty();
        assertThat(CorsConfig.parseOrigins("   ")).isEmpty();
        assertThat(CorsConfig.parseOrigins(null)).isEmpty();
    }

    @Test
    void parseOriginsTrimsAndSplitsCommaSeparatedValues() {
        assertThat(CorsConfig.parseOrigins(" https://app.example.com , https://admin.example.com "))
                .containsExactly("https://app.example.com", "https://admin.example.com");
    }
}
