package com.example.reservas.service;

import com.example.reservas.domain.Business;
import com.example.reservas.domain.Resource;
import com.example.reservas.repo.BusinessRepository;
import com.example.reservas.repo.ResourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceService {
    private final ResourceRepository resourceRepo;
    private final BusinessRepository businessRepo;

    public ResourceService(ResourceRepository resourceRepo, BusinessRepository businessRepo) {
        this.resourceRepo = resourceRepo; this.businessRepo = businessRepo;
    }

    @Transactional
    public Resource create(Long businessId, String name, int capacity) {
        Business b = businessRepo.findById(businessId)
                .orElseThrow(() -> new NotFoundException("Business %d no existe".formatted(businessId)));
        if (capacity <= 0) throw new ValidationException("capacity debe ser > 0");
        Resource r = new Resource();
        r.setBusiness(b); r.setName(name); r.setCapacity(capacity);
        return resourceRepo.save(r);
    }

    @Transactional(readOnly = true)
    public Resource get(Long id) {
        return resourceRepo.findById(id).orElseThrow(() -> new NotFoundException("Resource %d no existe".formatted(id)));
    }

    @Transactional(readOnly = true)
    public Page<Resource> listByBusiness(Long businessId, Pageable pageable) {
        return resourceRepo.findByBusinessId(businessId, pageable);
    }
}