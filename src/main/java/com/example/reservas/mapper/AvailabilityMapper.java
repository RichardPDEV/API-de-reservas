package com.example.reservas.mapper;

import com.example.reservas.dto.TimeWindowDto;
import com.example.reservas.service.AvailabilityService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper para convertir entre clases internas de servicios y DTOs de la API.
 * Evita exponer clases internas del dominio a la capa de presentación.
 */
@Component
public class AvailabilityMapper {

    /**
     * Convierte un TimeWindow interno del servicio a un DTO.
     *
     * @param window la ventana de tiempo del servicio
     * @return el DTO correspondiente
     * @throws IllegalArgumentException si window es null
     */
    public TimeWindowDto toDto(AvailabilityService.TimeWindow window) {
        if (window == null) {
            throw new IllegalArgumentException("TimeWindow no puede ser null");
        }
        return new TimeWindowDto(window.start(), window.end());
    }

    /**
     * Convierte una lista de TimeWindow internos a una lista de DTOs.
     *
     * @param windows la lista de ventanas de tiempo del servicio
     * @return la lista de DTOs correspondientes
     * @throws IllegalArgumentException si windows es null
     */
    public List<TimeWindowDto> toDtoList(List<AvailabilityService.TimeWindow> windows) {
        if (windows == null) {
            throw new IllegalArgumentException("La lista de TimeWindow no puede ser null");
        }
        return windows.stream()
                .map(this::toDto)
                .toList();
    }
}

