package com.example.reservas.repo;

import com.example.reservas.domain.Business;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessRepository extends JpaRepository<Business, Long> {
	List<Business> findByOwnerId(Long ownerId);
}