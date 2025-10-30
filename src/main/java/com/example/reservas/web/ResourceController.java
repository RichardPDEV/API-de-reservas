package com.example.reservas.web;

import com.example.reservas.domain.Resource;
import com.example.reservas.service.ResourceService;
import com.example.reservas.web.dto.CreateResourceRequest;
import com.example.reservas.web.dto.ResourceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
@Tag(name = "Resources", description = "Gestión de recursos reservables")
public class ResourceController {
    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) { this.resourceService = resourceService; }

    @PostMapping("/businesses/{businessId}/resources")
    @Operation(summary = "Crear recurso en un negocio")
    public ResourceResponse create(@PathVariable Long businessId, @Valid @RequestBody CreateResourceRequest req) {
        if (!businessId.equals(req.businessId())) {
            throw new com.example.reservas.service.ValidationException("businessId en path y body deben coincidir");
        }
        Resource r = resourceService.create(req.businessId(), req.name(), req.capacity());
        return toResponse(r);
    }

    @GetMapping("/resources/{id}")
    @Operation(summary = "Obtener recurso por id")
    public ResourceResponse get(@PathVariable Long id) {
        Resource r = resourceService.get(id);
        return toResponse(r);
    }

    @GetMapping("/resources")
    @Operation(summary = "Listar recursos por negocio (paginado)")
    public Page<ResourceResponse> list(@RequestParam Long businessId, Pageable pageable) {
        return resourceService.listByBusiness(businessId, pageable).map(this::toResponse);
    }

    private ResourceResponse toResponse(Resource r) {
        return new ResourceResponse(r.getId(), r.getBusiness().getId(), r.getName(), r.getCapacity());
    }
}
