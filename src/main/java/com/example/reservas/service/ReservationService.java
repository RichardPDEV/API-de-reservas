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

    public ReservationService(ReservationRepository reservationRepo, ResourceRepository resourceRepo, CancellationPolicyRepository cancellationPolicyRepo, CacheManager cacheManager) {
        this.reservationRepo = reservationRepo; this.resourceRepo = resourceRepo; this.cancellationPolicyRepo = cancellationPolicyRepo; this.cacheManager = cacheManager;
    }

    @Transactional
    public ReservationResponse create(CreateReservationRequest req) {
        if (req.startTime().isAfter(req.endTime()) || req.startTime().isEqual(req.endTime()))
            throw new ValidationException("startTime debe ser < endTime");

        Resource resource = resourceRepo.findById(req.resourceId())
                .orElseThrow(() -> new NotFoundException("Resource %d no existe".formatted(req.resourceId())));

        if (req.partySize() > resource.getCapacity())
            throw new ValidationException("partySize excede la capacidad del recurso");

        // Pre-chequeo de solape (amigable); la constraint en DB asegura consistencia bajo concurrencia
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

        Reservation saved = reservationRepo.saveAndFlush(r); // flush para surfear rápido la constraint
        return new ReservationResponse(
            saved.getId(), resource.getId(), saved.getCustomerName(), saved.getCustomerEmail(),
            saved.getPartySize(), saved.getStartTime(), saved.getEndTime(), saved.getStatus().name()
        );
    }

    @Transactional(readOnly = true)
    public Reservation getEntity(Long id) {
        return reservationRepo.findById(id).orElseThrow(() -> new NotFoundException("Reservation %d no existe".formatted(id)));
    }

    @Transactional(readOnly = true)
    public List<Reservation> listForDay(Long resourceId, LocalDate date) {
        OffsetDateTime start = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = start.plusDays(1);
        return reservationRepo.findForDay(resourceId, start, end);
    }

    /**
     * Página de reservas para un recurso en un intervalo [start, end) — usado por controladores paginados.
     */
    @Transactional(readOnly = true)
    public Page<Reservation> listPage(Long resourceId, OffsetDateTime start, OffsetDateTime end, Pageable pageable) {
        return reservationRepo.findByResourceIdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(resourceId, start, end, pageable);
    }

    /**
     * Cancela una reserva aplicando la política de cancelación del negocio si existe.
     * - Si se cancela dentro del período gratuito -> status = CANCELLED
     * - Si se cancela tarde -> status = LATE_CANCELLED
     */
    @Transactional
    public ReservationResponse cancel(Long id, String reason, OffsetDateTime now) {
        Reservation r = reservationRepo.findById(id).orElseThrow(() -> new NotFoundException("Reservation %d no existe".formatted(id)));
        if (r.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ValidationException("Reservation no está en estado CONFIRMED");
        }

        // obtener la política del negocio asociado al recurso
    Long businessId = r.getResource().getBusiness().getId();
    var policy = cancellationPolicyRepo.findFirstByBusinessId(businessId)
        .orElseThrow(() -> new NotFoundException("No hay política para business %d".formatted(businessId)));

        long minutesDiff = Duration.between(now, r.getStartTime()).toMinutes();
        boolean free = minutesDiff >= policy.getFreeBeforeMinutes();

        r.setStatus(free ? ReservationStatus.CANCELLED : ReservationStatus.LATE_CANCELLED);
        r.setCancellationReason(reason);
        Reservation saved = reservationRepo.saveAndFlush(r);

        // Invalidar caché de availability para el día de la reserva (UTC)
        OffsetDateTime startUtc = saved.getStartTime().withOffsetSameInstant(ZoneOffset.UTC);
        String day = startUtc.toLocalDate().toString(); // YYYY-MM-DD
        String key = "avail:" + saved.getResource().getId() + ":" + day;
        org.springframework.cache.Cache cache = cacheManager != null ? cacheManager.getCache("availability") : null;
        if (cache != null) {
            cache.evict(key);
        }

        return new ReservationResponse(
            saved.getId(), saved.getResource().getId(), saved.getCustomerName(), saved.getCustomerEmail(),
            saved.getPartySize(), saved.getStartTime(), saved.getEndTime(), saved.getStatus().name()
        );
    }
}