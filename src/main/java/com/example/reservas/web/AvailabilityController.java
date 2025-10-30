package com.example.reservas.web;

import com.example.reservas.service.AvailabilityService;
import com.example.reservas.web.dto.TimeWindowResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/availability")
@Tag(name = "Availability", description = "Consulta de disponibilidad")
public class AvailabilityController {
    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) { this.availabilityService = availabilityService; }

    @GetMapping
    @Operation(summary = "Ventanas libres por recurso y fecha")
    public List<TimeWindowResponse> get(@RequestParam Long resourceId, @RequestParam String date) {
        var windows = availabilityService.freeWindows(resourceId, LocalDate.parse(date));
        return windows.stream().map(w -> new TimeWindowResponse(w.start(), w.end())).toList();
    }
}
