package com.argos.backend_services.repository;

import com.argos.backend_services.entity.AnimalException;
import com.argos.backend_services.entity.Lot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnimalExceptionRepository extends JpaRepository<AnimalException, UUID> {
    List<AnimalException> findByLotId(Lot lotId);
}
