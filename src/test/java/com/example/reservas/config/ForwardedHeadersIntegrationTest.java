package com.example.reservas.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ForwardedHeadersIntegrationTest {

    private ForwardedHeaderFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ForwardedHeaderFilter();
        filter.setRelativeRedirects(true);
    }

    @Test
    void forwardedHeadersMakeRequestSecureBehindProxy() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health/liveness");
        request.setScheme("http");
        request.setSecure(false);
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("X-Forwarded-Host", "api.reservas.example.com");
        request.addHeader("X-Forwarded-Port", "443");

        AtomicReference<Boolean> secure = new AtomicReference<>();
        AtomicReference<String> scheme = new AtomicReference<>();
        AtomicReference<Integer> serverPort = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), captureRequest(secure, scheme, serverPort));

        assertThat(secure.get()).isTrue();
        assertThat(scheme.get()).isEqualTo("https");
        assertThat(serverPort.get()).isEqualTo(443);
    }

    @Test
    void directHttpRequestStaysInsecureWithoutForwardedProto() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health/liveness");
        request.setScheme("http");
        request.setSecure(false);

        AtomicReference<Boolean> secure = new AtomicReference<>();
        AtomicReference<String> scheme = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), captureRequest(secure, scheme, new AtomicReference<>()));

        assertThat(secure.get()).isFalse();
        assertThat(scheme.get()).isEqualTo("http");
    }

    private static FilterChain captureRequest(AtomicReference<Boolean> secure,
                                             AtomicReference<String> scheme,
                                             AtomicReference<Integer> serverPort) {
        return (req, res) -> {
            if (req instanceof HttpServletRequest httpRequest) {
                secure.set(httpRequest.isSecure());
                scheme.set(httpRequest.getScheme());
                serverPort.set(httpRequest.getServerPort());
            }
        };
    }
}
