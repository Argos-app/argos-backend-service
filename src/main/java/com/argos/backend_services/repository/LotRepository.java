package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Lot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LotRepository extends JpaRepository<Lot, UUID> {
    List<Lot> findByHerdId(UUID HerdId);
}
