package com.example.reservas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.mail.from:reservas@example.com}")
    private String fromAddress;

    @Value("${RESEND_API_KEY:${MAIL_PASSWORD:}}")
    private String resendApiKey;

    public EmailService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public boolean sendConfirmationCode(String to, String code) {
        try {
            String resolvedFrom = normalizeFromAddress(fromAddress);
            if (resendApiKey == null || resendApiKey.isBlank()) {
                log.error("Failed to send confirmation email: RESEND_API_KEY is not configured");
                return false;
            }

            String text = String.format("Tu código de confirmación es: %s\nSi no solicitaste este código, ignora este correo.", code);
            String requestBody = objectMapper.writeValueAsString(Map.of(
                "from", resolvedFrom,
                "to", new String[] { to },
                "subject", "Código de confirmación",
                "text", text
            ));
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + resendApiKey.trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.error("Failed to send confirmation email to {}: Resend returned HTTP {}", to, response.statusCode());
                return false;
            }
            log.info("Sent confirmation code from {} to {}", resolvedFrom, to);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Failed to send confirmation email to {}: request interrupted", to, ex);
            return false;
        } catch (Exception ex) {
            log.error("Failed to send confirmation email to {}", to, ex);
            return false;
        }
    }

    private String normalizeFromAddress(String configuredAddress) {
        if (configuredAddress == null || configuredAddress.isBlank()) {
            return "onboarding@resend.dev";
        }
        String trimmed = configuredAddress.trim();
        if (trimmed.contains("example.com") || trimmed.contains("gmail.com")) {
            return "onboarding@resend.dev";
        }
        return trimmed;
    }
}
