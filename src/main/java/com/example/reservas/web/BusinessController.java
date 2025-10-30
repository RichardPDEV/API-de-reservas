package com.example.reservas.web;

import com.example.reservas.domain.Business;
import com.example.reservas.repo.BusinessRepository;
import com.example.reservas.web.dto.BusinessResponse;
import com.example.reservas.web.dto.CreateBusinessRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/businesses")
@Tag(name = "Businesses", description = "Gestión de negocios")
public class BusinessController {
    private final BusinessRepository businessRepo;

    public BusinessController(BusinessRepository businessRepo) { this.businessRepo = businessRepo; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear negocio")
    public BusinessResponse create(@Valid @RequestBody CreateBusinessRequest req) {
        Business b = new Business();
        b.setName(req.name()); b.setType(req.type());
        b = businessRepo.save(b);
        return new BusinessResponse(b.getId(), b.getName(), b.getType());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener negocio por id")
    public BusinessResponse get(@PathVariable Long id) {
        Business b = businessRepo.findById(id).orElseThrow(() -> new com.example.reservas.service.NotFoundException("Business %d no existe".formatted(id)));
        return new BusinessResponse(b.getId(), b.getName(), b.getType());
    }
}
