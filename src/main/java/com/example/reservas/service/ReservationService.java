package com.example.reservas.service;

import com.example.reservas.domain.*;
import com.example.reservas.dto.CreateReservationRequest;
import com.example.reservas.dto.ReservationResponse;
import com.example.reservas.repo.ReservationRepository;
import com.example.reservas.repo.ResourceRepository;
import com.example.reservas.repo.CancellationPolicyRepository;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Service
public class ReservationService {
    private final ReservationRepository reservationRepo;
    private final ResourceRepository resourceRepo;
    private final CancellationPolicyRepository cancellationPolicyRepo;
    private final CacheManager cacheManager;

    public ReservationService(ReservationRepository reservationRepo, ResourceRepository resourceRepo,
                              CancellationPolicyRepository cancellationPolicyRepo, CacheManager cacheManager) {
        this.reservationRepo = reservationRepo;
        this.resourceRepo = resourceRepo;
        this.cancellationPolicyRepo = cancellationPolicyRepo;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public ReservationResponse create(CreateReservationRequest req) {
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
        
        // Evict cache entries for availability
        var cache = cacheManager.getCache("availability");
        if (cache != null) {
            String key1 = "avail:" + resource.getId() + ":" + dayKey(req.startTime());
            cache.evict(key1);
            if (!req.startTime().toLocalDate().equals(req.endTime().toLocalDate())) {
                String key2 = "avail:" + resource.getId() + ":" + dayKey(req.endTime());
                cache.evict(key2);
            }
        }
        
        return new ReservationResponse(
            saved.getId(), resource.getId(), saved.getCustomerName(), saved.getCustomerEmail(),
            saved.getPartySize(), saved.getStartTime(), saved.getEndTime(), saved.getStatus().name()
        );
    }

    // Helper para clave de día (UTC)
    public String dayKey(OffsetDateTime ts) {
        return ts.atZoneSameInstant(ZoneOffset.UTC).toLocalDate().toString();
    }
    
    // Obtiene la entidad Reservation o lanza NotFoundException
    public Reservation getEntity(Long id) {
        return reservationRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation %d no existe".formatted(id)));
    }

    // Lista paginada para un día
    public Page<Reservation> listPage(Long resourceId, OffsetDateTime start, OffsetDateTime end, Pageable pageable) {
        return reservationRepo.findForDayPage(resourceId, start, end, pageable);
    }

    // Cancelación aplicando política y limpiando cache de availability
    @Transactional
    public ReservationResponse cancel(Long id, String reason, OffsetDateTime now) {
        Reservation r = getEntity(id);

        if (r.getStatus() != ReservationStatus.CONFIRMED)
            throw new ValidationException("Reserva no está en estado CONFIRMED");

        // obtener política de cancelación del negocio del recurso
        CancellationPolicy policy = cancellationPolicyRepo
                .findFirstByBusinessId(r.getResource().getBusiness().getId())
                .orElse(null);

    Integer freeBeforeInt = null;
    if (policy != null) freeBeforeInt = policy.getFreeBeforeMinutes();
    int freeBefore = freeBeforeInt == null ? 0 : freeBeforeInt;

        long minutesBefore = Duration.between(now, r.getStartTime()).toMinutes();

        if (minutesBefore >= freeBefore) {
            r.setStatus(ReservationStatus.CANCELLED);
        } else {
            r.setStatus(ReservationStatus.LATE_CANCELLED);
        }

        r.setCancellationReason(reason);

        Reservation saved = reservationRepo.saveAndFlush(r);

        // Evict cache entries for availability for start day and end day (si distinto)
        var cache = cacheManager.getCache("availability");
        if (cache != null) {
            String key1 = "avail:" + saved.getResource().getId() + ":" + dayKey(saved.getStartTime());
            cache.evict(key1);
            if (!saved.getStartTime().toLocalDate().equals(saved.getEndTime().toLocalDate())) {
                String key2 = "avail:" + saved.getResource().getId() + ":" + dayKey(saved.getEndTime());
                cache.evict(key2);
            }
        }

        return new ReservationResponse(
                saved.getId(), saved.getResource().getId(), saved.getCustomerName(), saved.getCustomerEmail(),
                saved.getPartySize(), saved.getStartTime(), saved.getEndTime(), saved.getStatus().name()
        );
    }

}
