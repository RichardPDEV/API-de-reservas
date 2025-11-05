package com.example.reservas.controller;

import com.example.reservas.dto.ReservationResponse;
import com.example.reservas.dto.TimeWindowDto;
import com.example.reservas.mapper.AvailabilityMapper;
import com.example.reservas.service.AvailabilityService;
import com.example.reservas.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador para consultas de recursos (reservas y disponibilidad).
 * Agrupa endpoints relacionados con la consulta de información de recursos.
 */
@RestController
@RequestMapping("/api/resources")
@Tag(name = "Resource Queries", description = "Consultas de recursos: reservas y disponibilidad")
public class ResourceQueryController {

    private final ReservationService reservationService;
    private final AvailabilityService availabilityService;
    private final AvailabilityMapper availabilityMapper;

    public ResourceQueryController(
            ReservationService reservationService,
            AvailabilityService availabilityService,
            AvailabilityMapper availabilityMapper) {
        this.reservationService = reservationService;
        this.availabilityService = availabilityService;
        this.availabilityMapper = availabilityMapper;
    }

    /**
     * Lista todas las reservas de un recurso para un día específico.
     *
     * @param resourceId ID del recurso
     * @param date fecha en formato YYYY-MM-DD
     * @return lista de reservas del día
     */
    @GetMapping("/{resourceId}/reservations")
    @Operation(
            summary = "Listar reservas por recurso y fecha",
            description = "Obtiene todas las reservas de un recurso para un día específico (formato YYYY-MM-DD)"
    )
    public List<ReservationResponse> listReservationsForDay(
            @Parameter(description = "ID del recurso", required = true, example = "1")
            @PathVariable @NotNull Long resourceId,
            
            @Parameter(description = "Fecha en formato YYYY-MM-DD", required = true, example = "2025-01-15")
            @RequestParam("date") 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            @NotNull LocalDate date
    ) {
        return reservationService.listForDay(resourceId, date);
    }

    /**
     * Lista las ventanas de tiempo disponibles para un recurso en un día específico.
     *
     * @param resourceId ID del recurso
     * @param date fecha en formato YYYY-MM-DD
     * @return lista de ventanas de tiempo disponibles
     */
    @GetMapping("/{resourceId}/availability")
    @Operation(
            summary = "Consultar disponibilidad por recurso y fecha",
            description = "Obtiene las ventanas de tiempo disponibles para un recurso en un día específico (formato YYYY-MM-DD)"
    )
    public List<TimeWindowDto> listAvailabilityForDay(
            @Parameter(description = "ID del recurso", required = true, example = "1")
            @PathVariable @NotNull Long resourceId,
            
            @Parameter(description = "Fecha en formato YYYY-MM-DD", required = true, example = "2025-01-15")
            @RequestParam("date") 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            @NotNull LocalDate date
    ) {
        var windows = availabilityService.freeWindows(resourceId, date);
        return availabilityMapper.toDtoList(windows);
    }
}

