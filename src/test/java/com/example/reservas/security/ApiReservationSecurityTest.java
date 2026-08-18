package com.example.reservas.security;

import com.example.reservas.controller.ReservationController;
import com.example.reservas.dto.ReservationResponse;
import com.example.reservas.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = ApiReservationSecurityTest.MvcTestConfig.class)
@AutoConfigureMockMvc(addFilters = true)
class ApiReservationSecurityTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({ReservationController.class, SecurityConfig.class, JwtFilter.class})
    static class MvcTestConfig {
    }

    private static final String CREATE_BODY = """
            {
              "resourceId": 1,
              "customerName": "Ana",
              "customerEmail": "ana@example.com",
              "partySize": 2,
              "tableId": "T1",
              "startTime": "2026-07-11T20:00:00Z",
              "endTime": "2026-07-11T22:00:00Z"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void postApiReservationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postApiReservationRequiresUserOrOwnerRole() throws Exception {
        when(jwtUtil.validate("no-role-token")).thenReturn(true);
        when(jwtUtil.extractUsername("no-role-token")).thenReturn("guest@example.com");
        when(jwtUtil.extractRole("no-role-token")).thenReturn(null);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY)
                        .header("Authorization", "Bearer no-role-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postApiReservationAllowsUserRole() throws Exception {
        when(jwtUtil.validate("user-token")).thenReturn(true);
        when(jwtUtil.extractUsername("user-token")).thenReturn("ana@example.com");
        when(jwtUtil.extractRole("user-token")).thenReturn("USER");
        when(reservationService.create(any(), eq("ana@example.com"))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY)
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isCreated());
    }

    @Test
    void postApiReservationAllowsOwnerRole() throws Exception {
        when(jwtUtil.validate("owner-token")).thenReturn(true);
        when(jwtUtil.extractUsername("owner-token")).thenReturn("owner@example.com");
        when(jwtUtil.extractRole("owner-token")).thenReturn("OWNER");
        when(reservationService.create(any(), eq("owner@example.com"))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY)
                        .header("Authorization", "Bearer owner-token"))
                .andExpect(status().isCreated());
    }

    private ReservationResponse sampleResponse() {
        return new ReservationResponse(
                1L,
                1L,
                "T1",
                7L,
                "Ana",
                "ana@example.com",
                2,
                OffsetDateTime.parse("2026-07-11T20:00:00Z"),
                OffsetDateTime.parse("2026-07-11T22:00:00Z"),
                "CONFIRMED",
                "Restaurante Demo",
                "T1",
                null,
                OffsetDateTime.parse("2026-07-10T19:00:00Z")
        );
    }
}
