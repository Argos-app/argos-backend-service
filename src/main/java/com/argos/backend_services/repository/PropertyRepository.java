package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, UUID> {
    List<Property> findByUserIdAndActiveTrue(UUID userId);

    List<Property> findByUser_IdAndActiveTrueOrderByNameAsc(UUID administratorId);

    Page<Property> findByUser_IdAndActiveTrue(UUID administratorId, Pageable pageable);

    Optional<Property> findByIdAndUser_Id(UUID propertyId, UUID administratorId);

    boolean existsByUser_Id(UUID userId);
}
