package com.example.reservas.repo;

import com.example.reservas.domain.CancellationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, Long> {
    Optional<CancellationPolicy> findFirstByBusinessId(Long businessId);
}