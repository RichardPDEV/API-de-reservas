package com.example.reservas.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Component
@Profile("prod")
public class ProdStartupValidator {

    private static final Set<String> INSECURE_DB_PASSWORDS = Set.of(
            "changeme", "password", "postgres", "reservas"
    );

    private static final Set<String> INSECURE_MAIL_PASSWORDS = Set.of(
            "changeme", "password", "your-resend-api-key", "apikey"
    );

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final String jwtSecret;
    private final String corsAllowedOrigins;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final String mailHost;
    private final String mailPassword;
    private final String mailFrom;

    public ProdStartupValidator(@Value("${DB_URL:}") String dbUrl,
                                @Value("${DB_USERNAME:}") String dbUser,
                                @Value("${DB_PASSWORD:}") String dbPassword,
                                @Value("${APP_JWT_SECRET:}") String jwtSecret,
                                @Value("${APP_CORS_ALLOWED_ORIGINS:}") String corsAllowedOrigins,
                                @Value("${APP_COOKIE_SECURE:false}") boolean cookieSecure,
                                @Value("${APP_COOKIE_SAMESITE:}") String cookieSameSite,
                                @Value("${MAIL_HOST:}") String mailHost,
                                @Value("${MAIL_PASSWORD:}") String mailPassword,
                                @Value("${MAIL_FROM:}") String mailFrom) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.jwtSecret = jwtSecret;
        this.corsAllowedOrigins = corsAllowedOrigins;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.mailHost = mailHost;
        this.mailPassword = mailPassword;
        this.mailFrom = mailFrom;
    }

    @PostConstruct
    void validateProdSecrets() {
        validateRequiredSettings(
                dbUrl, dbUser, dbPassword, jwtSecret, corsAllowedOrigins, cookieSecure, cookieSameSite,
                mailHost, mailPassword, mailFrom
        );
    }

    static void validateRequiredSettings(String dbUrl,
                                         String dbUser,
                                         String dbPassword,
                                         String jwtSecret,
                                         String corsAllowedOrigins,
                                         boolean cookieSecure,
                                         String cookieSameSite,
                                         String mailHost,
                                         String mailPassword,
                                         String mailFrom) {
        if (isBlank(dbUrl) || isBlank(dbUser) || isBlank(dbPassword) || isBlank(jwtSecret)) {
            throw new IllegalStateException(
                    "Producción requiere DB_URL, DB_USERNAME, DB_PASSWORD y APP_JWT_SECRET"
            );
        }
        if (isBlank(corsAllowedOrigins)) {
            throw new IllegalStateException(
                    "Producción requiere APP_CORS_ALLOWED_ORIGINS con el origen HTTPS del frontend"
            );
        }
        if (isBlank(cookieSameSite)) {
            throw new IllegalStateException(
                    "Producción requiere APP_COOKIE_SAMESITE (None si frontend y API están en dominios distintos)"
            );
        }

        String normalizedPassword = dbPassword.trim().toLowerCase(Locale.ROOT);
        if (INSECURE_DB_PASSWORDS.contains(normalizedPassword)) {
            throw new IllegalStateException("DB_PASSWORD no puede usar un valor por defecto inseguro en producción");
        }

        validateJwtSecret(jwtSecret);
        validateCorsOrigins(corsAllowedOrigins);
        validateCookieSettings(cookieSecure, cookieSameSite);
        validateMailSettings(mailHost, mailPassword, mailFrom);
    }

    private static void validateMailSettings(String mailHost, String mailPassword, String mailFrom) {
        if (isBlank(mailHost)) {
            throw new IllegalStateException("Producción requiere MAIL_HOST");
        }
        if (mailHost.equalsIgnoreCase("localhost") || mailHost.startsWith("127.0.0.1")) {
            throw new IllegalStateException("Producción no permite MAIL_HOST de desarrollo: " + mailHost);
        }
        if (isBlank(mailPassword)) {
            throw new IllegalStateException("Producción requiere MAIL_PASSWORD");
        }
        String normalizedMailPassword = mailPassword.trim().toLowerCase(Locale.ROOT);
        if (INSECURE_MAIL_PASSWORDS.contains(normalizedMailPassword)) {
            throw new IllegalStateException("MAIL_PASSWORD no puede usar un valor placeholder en producción");
        }
        if (isBlank(mailFrom)) {
            throw new IllegalStateException("Producción requiere MAIL_FROM con un dominio verificado");
        }
        String normalizedMailFrom = mailFrom.trim().toLowerCase(Locale.ROOT);
        if (normalizedMailFrom.contains("example.com") || normalizedMailFrom.contains("reservas@example")) {
            throw new IllegalStateException("MAIL_FROM debe ser una dirección real verificada en producción");
        }
        if (!normalizedMailFrom.contains("@") || normalizedMailFrom.startsWith("@") || normalizedMailFrom.endsWith("@")) {
            throw new IllegalStateException("MAIL_FROM debe ser una dirección de correo válida");
        }
    }

    private static void validateJwtSecret(String jwtSecret) {
        if (jwtSecret.length() < 32 || jwtSecret.startsWith("replace-with") || jwtSecret.contains("dev-secret")) {
            throw new IllegalStateException(
                    "APP_JWT_SECRET debe ser una cadena segura de al menos 32 caracteres en producción"
            );
        }
    }

    private static void validateCorsOrigins(String corsAllowedOrigins) {
        String[] origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);

        if (origins.length == 0) {
            throw new IllegalStateException("APP_CORS_ALLOWED_ORIGINS debe incluir al menos un origen HTTPS");
        }

        for (String origin : origins) {
            if (!origin.startsWith("https://")) {
                throw new IllegalStateException(
                        "Producción exige orígenes CORS HTTPS; origen inválido: " + origin
                );
            }
            if (origin.contains("localhost") || origin.contains("127.0.0.1")) {
                throw new IllegalStateException(
                        "Producción no permite orígenes CORS de desarrollo: " + origin
                );
            }
        }
    }

    private static void validateCookieSettings(boolean cookieSecure, String cookieSameSite) {
        if (!cookieSecure) {
            throw new IllegalStateException("Producción requiere APP_COOKIE_SECURE=true (HTTPS obligatorio)");
        }

        String normalizedSameSite = cookieSameSite.trim();
        if (!Set.of("None", "Strict", "Lax").contains(normalizedSameSite)) {
            throw new IllegalStateException("APP_COOKIE_SAMESITE debe ser None, Strict o Lax en producción");
        }
        if ("None".equalsIgnoreCase(normalizedSameSite) && !cookieSecure) {
            throw new IllegalStateException("APP_COOKIE_SAMESITE=None requiere APP_COOKIE_SECURE=true");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
