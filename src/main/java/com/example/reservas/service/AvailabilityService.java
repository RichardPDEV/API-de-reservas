package com.example.reservas.service;

import com.example.reservas.domain.Reservation;
import com.example.reservas.domain.ValidationException;
import com.example.reservas.repo.ReservationRepository;
import org.springframework.cache.annotation.Cacheable; // Caché de disponibilidad por día
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class AvailabilityService {

    private final ReservationRepository reservationRepo;

    public AvailabilityService(ReservationRepository reservationRepo) {
        this.reservationRepo = reservationRepo;
    }

    public record TimeWindow(OffsetDateTime start, OffsetDateTime end) {}

    /**
     * Dev A: Verifica availability (caché)
     * Ejemplo:
     *   availabilityService.freeWindows(resourceId, LocalDate.parse("2025-01-01"));
     *
     * Calcula ventanas libres para el recurso en el día dado (UTC) usando caché.
     * Clave de caché: "avail:{resourceId}:{YYYY-MM-DD}"
     */
    @Cacheable(cacheNames = "availability", key = "'avail:' + #resourceId + ':' + #date.toString()")
    @Transactional(readOnly = true)
    public List<TimeWindow> freeWindows(Long resourceId, LocalDate date) {
        if (resourceId == null) throw new ValidationException("resourceId es requerido");
        if (date == null) throw new ValidationException("date es requerido");

        // Día en UTC [00:00, 24:00)
        OffsetDateTime dayStart = date.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime dayEnd = dayStart.plusDays(1);

        // Traer reservas que impactan el día
        List<Reservation> reservations = reservationRepo.findForDay(resourceId, dayStart, dayEnd);

        // Ordenar por inicio para calcular correctamente las ventanas libres
        reservations.sort(Comparator.comparing(Reservation::getStartTime));

        List<TimeWindow> result = new ArrayList<>();
        OffsetDateTime cursor = dayStart;

        for (Reservation r : reservations) {
            // Recorta la reserva al rango del día
            OffsetDateTime rs = r.getStartTime();
            OffsetDateTime re = r.getEndTime();

            OffsetDateTime rsClamped = rs.isAfter(dayStart) ? rs : dayStart;
            OffsetDateTime reClamped = re.isBefore(dayEnd) ? re : dayEnd;

            // Si la reserva no cae dentro del día, saltar
            if (!reClamped.isAfter(rsClamped)) continue;

            // Si hay hueco entre el cursor y el inicio de la reserva, añadirlo (clamp al fin del día)
            if (cursor.isBefore(rsClamped)) {
                OffsetDateTime gapEnd = rsClamped.isBefore(dayEnd) ? rsClamped : dayEnd;
                if (cursor.isBefore(gapEnd)) {
                    result.add(new TimeWindow(cursor, gapEnd));
                }
            }

            // Mover cursor al final de la reserva (máximo para manejar solapes)
            if (reClamped.isAfter(cursor)) {
                cursor = reClamped;
            }

            // Si ya llegamos al final del día, terminar
            if (!cursor.isBefore(dayEnd)) break;
        }

        // Último hueco del día
        if (cursor.isBefore(dayEnd)) {
            result.add(new TimeWindow(cursor, dayEnd));
        }

        return result;
    }
}