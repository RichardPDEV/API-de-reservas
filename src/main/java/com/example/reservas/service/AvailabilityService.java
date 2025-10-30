package com.example.reservas.service;

import com.example.reservas.domain.Reservation;
import com.example.reservas.repo.ReservationRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class AvailabilityService {

    private final ReservationRepository reservationRepo;

    public AvailabilityService(ReservationRepository reservationRepo) {
        this.reservationRepo = reservationRepo;
    }

    // DTO para ventanas libres; serializable para cache Redis
    public record TimeWindow(OffsetDateTime start, OffsetDateTime end) implements Serializable {}

    /**
     * Calcula ventanas libres de un recurso para un día (UTC) y las cachea en Redis.
     * Clave: "avail:{resourceId}:{YYYY-MM-DD}"
     */
    @Cacheable(cacheNames = "availability", key = "'avail:' + #resourceId + ':' + #date")
    @Transactional(readOnly = true)
    public List<TimeWindow> freeWindows(Long resourceId, LocalDate date) {
        // Día en UTC [00:00, 24:00)
        OffsetDateTime dayStart = date.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime dayEnd = dayStart.plusDays(1);

        List<Reservation> reservations = reservationRepo.findForDay(resourceId, dayStart, dayEnd);
        List<TimeWindow> result = new ArrayList<>();

        OffsetDateTime cursor = dayStart;
        for (Reservation r : reservations) {
            if (cursor.isBefore(r.getStartTime())) {
                result.add(new TimeWindow(cursor, r.getStartTime()));
            }
            if (r.getEndTime().isAfter(cursor)) {
                cursor = r.getEndTime();
            }
        }

        if (cursor.isBefore(dayEnd)) {
            result.add(new TimeWindow(cursor, dayEnd));
        }

        return result;
    }
}