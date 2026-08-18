package com.example.reservas.web.error;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void prodProfileReturnsGenericInternalErrorWithoutInternalDetails() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        GlobalExceptionHandler handler = new GlobalExceptionHandler(environment);

        Exception ex = new RuntimeException("secret db password leaked");
        ResponseEntity<ApiError> response = handler.handleOthers(ex, webRequest("/api/reservations"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("Error interno del servidor");
        assertThat(body.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.details()).doesNotContainKeys("exception", "message", "cause");
    }

    @Test
    void prodProfileStillIncludesTraceIdWhenPresent() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        GlobalExceptionHandler handler = new GlobalExceptionHandler(environment);
        MDC.put("traceId", "trace-abc-123");

        ResponseEntity<ApiError> response = handler.handleOthers(
                new RuntimeException("internal failure"),
                webRequest("/api/reservations"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().details()).containsEntry("traceId", "trace-abc-123");
        assertThat(response.getBody().details()).doesNotContainKeys("exception", "message", "cause");
    }

    @Test
    void devProfileReturnsVerboseInternalErrorDetails() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(new MockEnvironment());

        RuntimeException cause = new IllegalStateException("root cause detail");
        Exception ex = new RuntimeException("visible failure", cause);
        ResponseEntity<ApiError> response = handler.handleOthers(ex, webRequest("/api/reservations"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("Error inesperado: RuntimeException");
        assertThat(body.details())
                .containsEntry("exception", "java.lang.RuntimeException")
                .containsEntry("message", "visible failure")
                .containsEntry("cause", "root cause detail");
    }

    private static ServletWebRequest webRequest(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return new ServletWebRequest(request);
    }
}
