package com.example.reservas.repo;

import com.example.reservas.domain.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    Page<Resource> findByBusinessId(Long businessId, Pageable pageable);
}