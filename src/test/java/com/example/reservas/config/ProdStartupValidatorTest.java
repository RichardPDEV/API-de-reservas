package com.example.reservas.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProdStartupValidatorTest {

    private static final String VALID_JWT = "prod-secret-with-enough-length-1234567890";
    private static final String VALID_CORS = "https://reservas.example.com";
    private static final String VALID_MAIL_HOST = "smtp.resend.com";
    private static final String VALID_MAIL_PASSWORD = "re_live_secret_key_abc123xyz";
    private static final String VALID_MAIL_FROM = "reservas@mycompany.com";

    @Test
    void acceptsValidProductionSettings() {
        assertThatCode(() -> ProdStartupValidator.validateRequiredSettings(
                "jdbc:postgresql://db:5432/reservas",
                "reservas",
                "super-secure-password",
                VALID_JWT,
                VALID_CORS,
                true,
                "None",
                VALID_MAIL_HOST,
                VALID_MAIL_PASSWORD,
                VALID_MAIL_FROM
        )).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingDatabaseVariables() {
        assertThatThrownBy(() -> ProdStartupValidator.validateRequiredSettings(
                "",
                "reservas",
                "super-secure-password",
                VALID_JWT,
                VALID_CORS,
                true,
                "None",
                VALID_MAIL_HOST,
                VALID_MAIL_PASSWORD,
                VALID_MAIL_FROM
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_URL");
    }

    @Test
    void rejectsMissingJwtSecret() {
        assertThatThrownBy(() -> ProdStartupValidator.validateRequiredSettings(
                "jdbc:postgresql://db:5432/reservas",
                "reservas",
                "super-secure-password",
                "",
                VALID_CORS,
                true,
                "None",
                VALID_MAIL_HOST,
                VALID_MAIL_PASSWORD,
                VALID_MAIL_FROM
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    void rejectsInsecureDatabasePassword() {
        assertThatThrownBy(() -> ProdStartupValidator.validateRequiredSettings(
                "jdbc:postgresql://db:5432/reservas",
                "reservas",
                "changeme",
                VALID_JWT,
                VALID_CORS,
                true,
                "None",
                VALID_MAIL_HOST,
                VALID_MAIL_PASSWORD,
                VALID_MAIL_FROM
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_PASSWORD");
    }

    @Test
    void rejectsHttpCorsOrigin() {
        assertThatThrownBy(() -> ProdStartupValidator.validateRequiredSettings(
                "jdbc:postgresql://db:5432/reservas",
                "reservas",
                "super-secure-password",
                VALID_JWT,
                "http://reservas.example.com",
                true,
                "None",
                VALID_MAIL_HOST,
                VALID_MAIL_PASSWORD,
                VALID_MAIL_FROM
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void rejectsLocalhostCorsOrigin() {
        assertThatThrownBy(() -> ProdStartupValidator.validateRequiredSettings(
                "jdbc:postgresql://db:5432/reservas",
                "reservas",
                "super-secure-password",
                VALID_JWT,
                "https://localhost:5173",
                true,
                "None",
                VALID_MAIL_HOST,
                VALID_MAIL_PASSWORD,
                VALID_MAIL_FROM
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("desarrollo");
    }

    @Test
    void rejectsInsecureCookies() {
        assertThatThrownBy(() -> ProdStartupValidator.validateRequiredSettings(
                "jdbc:postgresql://db:5432/reservas",
                "reservas",
                "super-secure-password",
                VALID_JWT,
                VALID_CORS,
                false,
                "None",
                VALID_MAIL_HOST,
                VALID_MAIL_PASSWORD,
                VALID_MAIL_FROM
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_COOKIE_SECURE=true");
    }

    @Test
    void rejectsMissingCorsOrigins() {
        assertThatThrownBy(() -> ProdStartupValidator.validateRequiredSettings(
                "jdbc:postgresql://db:5432/reservas",
                "reservas",
                "super-secure-password",
                VALID_JWT,
                "",
                true,
                "None",
                VALID_MAIL_HOST,
                VALID_MAIL_PASSWORD,
                VALID_MAIL_FROM
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_CORS_ALLOWED_ORIGINS");
    }

    @Test
    void rejectsMissingMailPassword() {
        assertThatThrownBy(() -> ProdStartupValidator.validateRequiredSettings(
                "jdbc:postgresql://db:5432/reservas",
                "reservas",
                "super-secure-password",
                VALID_JWT,
                VALID_CORS,
                true,
                "None",
                VALID_MAIL_HOST,
                "",
                VALID_MAIL_FROM
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAIL_PASSWORD");
    }

    @Test
    void rejectsPlaceholderMailPassword() {
        assertThatThrownBy(() -> ProdStartupValidator.validateRequiredSettings(
                "jdbc:postgresql://db:5432/reservas",
                "reservas",
                "super-secure-password",
                VALID_JWT,
                VALID_CORS,
                true,
                "None",
                VALID_MAIL_HOST,
                "your-resend-api-key",
                VALID_MAIL_FROM
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAIL_PASSWORD");
    }

    @Test
    void rejectsExampleMailFrom() {
        assertThatThrownBy(() -> ProdStartupValidator.validateRequiredSettings(
                "jdbc:postgresql://db:5432/reservas",
                "reservas",
                "super-secure-password",
                VALID_JWT,
                VALID_CORS,
                true,
                "None",
                VALID_MAIL_HOST,
                VALID_MAIL_PASSWORD,
                "reservas@example.com"
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAIL_FROM");
    }

    @Test
    void rejectsLocalhostMailHost() {
        assertThatThrownBy(() -> ProdStartupValidator.validateRequiredSettings(
                "jdbc:postgresql://db:5432/reservas",
                "reservas",
                "super-secure-password",
                VALID_JWT,
                VALID_CORS,
                true,
                "None",
                "localhost",
                VALID_MAIL_PASSWORD,
                VALID_MAIL_FROM
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAIL_HOST");
    }
}
