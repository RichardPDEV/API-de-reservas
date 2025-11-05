package com.example.reservas.reservations;

import com.example.reservas.domain.Resource;
import com.example.reservas.domain.ValidationException;
import com.example.reservas.dto.CreateReservationRequest;
import com.example.reservas.repo.ResourceRepository;
import com.example.reservas.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CapacityIT {

  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    postgres.start();
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
    // El test no necesita Redis; si lo usas, añade aquí host/port.
  }

  @Autowired ReservationService reservationService;
  @Autowired ResourceRepository resourceRepo;

  Resource resource;

  @BeforeEach
  void setup() {
    resource = new Resource();
    resource.setName("Mesa 1");
    resource.setCapacity(4);
    // Si tu entidad requiere business, setéalo aquí
    resource = resourceRepo.saveAndFlush(resource);
  }

  @Test
  void create_allowsEqualToCapacity() {
    var start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
    var req = new CreateReservationRequest(
        resource.getId(),
        "Juan",
        "juan@example.com",
        4,
        start,
        start.plusHours(2)
    );
    assertDoesNotThrow(() -> reservationService.create(req));
  }

  @Test
  void create_rejectsOverCapacity() {
    var start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
    var req = new CreateReservationRequest(
        resource.getId(),
        "Richard",
        "richard@example.com",
        5,
        start,
        start.plusHours(2)
    );
    assertThrows(ValidationException.class, () -> reservationService.create(req));
  }
}