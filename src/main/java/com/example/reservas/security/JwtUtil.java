package com.example.reservas.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.Date;

@Component
public class JwtUtil {
    private static final String DEV_FALLBACK_SECRET =
            "dev-only-insecure-jwt-secret-do-not-use-in-prod!!";

    private final Key key;
    private final long accessValidityMs;
    private final long refreshValidityMs;

    public JwtUtil(
            Environment environment,
            @Value("${APP_JWT_SECRET:}") String secret,
            @Value("${APP_JWT_ACCESS_VALIDITY_MS:900000}") long accessValidityMs,
            @Value("${APP_JWT_REFRESH_VALIDITY_MS:604800000}") long refreshValidityMs) {
        this.key = Keys.hmacShaKeyFor(resolveSecret(environment, secret).getBytes(StandardCharsets.UTF_8));
        this.accessValidityMs = accessValidityMs;
        this.refreshValidityMs = refreshValidityMs;
    }

    private static String resolveSecret(Environment environment, String secret) {
        if (isProdProfile(environment)) {
            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException(
                        "APP_JWT_SECRET es obligatorio en producción"
                );
            }
            if (secret.length() < 32 || secret.startsWith("replace-with") || secret.contains("dev-secret")) {
                throw new IllegalStateException(
                        "APP_JWT_SECRET debe ser una cadena segura de al menos 32 caracteres en producción"
                );
            }
            return secret;
        }

        if (secret == null || secret.isBlank() || secret.startsWith("replace-with")) {
            return DEV_FALLBACK_SECRET;
        }
        return secret;
    }

    private static boolean isProdProfile(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("prod"));
    }

    public String generateAccessToken(String username) {
        return generateToken(username, accessValidityMs, null);
    }

    public String generateAccessToken(String username, String role) {
        return generateToken(username, accessValidityMs, role);
    }

    public String generateRefreshToken(String username) {
        return generateToken(username, refreshValidityMs, null);
    }

    private String generateToken(String username, long validityMs, String role) {
        Date now = new Date();
        var builder = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + validityMs))
                .signWith(key);
        if (role != null && !role.isBlank()) {
            builder.claim("role", role);
        }
        return builder.compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validate(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String extractRole(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("role", String.class);
    }
}
