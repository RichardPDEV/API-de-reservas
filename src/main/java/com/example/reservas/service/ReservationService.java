package com.example.reservas.service;

import com.example.reservas.domain.*;
import com.example.reservas.dto.CreateReservationRequest;
import com.example.reservas.dto.ReservationResponse;
import com.example.reservas.repo.ReservationRepository;
import com.example.reservas.repo.ResourceRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching; // Importa la anotación Caching
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Service
public class ReservationService {
    private final ReservationRepository reservationRepo;
    private final ResourceRepository resourceRepo;

    public ReservationService(ReservationRepository reservationRepo, ResourceRepository resourceRepo) {
        this.reservationRepo = reservationRepo; this.resourceRepo = resourceRepo;
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "availability", key = "'avail:' + #req.resourceId() + ':' + #root.target.dayKey(#req.startTime())"),
        @CacheEvict(cacheNames = "availability", key = "'avail:' + #req.resourceId() + ':' + #root.target.dayKey(#req.endTime())", 
                    condition = "#req.startTime().toLocalDate() != #req.endTime().toLocalDate()")
    })
    @Transactional
    public ReservationResponse create(CreateReservationRequest req) {
        // ... (el resto de la implementación del método create es el mismo)
        if (req.startTime().isAfter(req.endTime()) || req.startTime().isEqual(req.endTime()))
            throw new ValidationException("startTime debe ser < endTime");

        Resource resource = resourceRepo.findById(req.resourceId())
                .orElseThrow(() -> new NotFoundException("Resource %d no existe".formatted(req.resourceId())));

        if (req.partySize() > resource.getCapacity())
            throw new ValidationException("partySize excede la capacidad del recurso");

        List<Reservation> overlaps = reservationRepo.findOverlaps(resource.getId(), req.startTime(), req.endTime());
        if (!overlaps.isEmpty())
            throw new ValidationException("Ya existe una reserva que solapa ese horario");

        Reservation r = new Reservation();
        r.setResource(resource);
        r.setCustomerName(req.customerName());
        r.setCustomerEmail(req.customerEmail());
        r.setPartySize(req.partySize());
        r.setStartTime(req.startTime());
        r.setEndTime(req.endTime());
        r.setStatus(ReservationStatus.CONFIRMED);

        Reservation saved = reservationRepo.saveAndFlush(r);
        return new ReservationResponse(
            saved.getId(), resource.getId(), saved.getCustomerName(), saved.getCustomerEmail(),
            saved.getPartySize(), saved.getStartTime(), saved.getEndTime(), saved.getStatus().name()
        );
    }

    // Helper para clave de día (UTC)
    public String dayKey(OffsetDateTime ts) {
        return ts.atZoneSameInstant(ZoneOffset.UTC).toLocalDate().toString();
    }
    
    // ... (el resto de los métodos de la clase son los mismos)
}
