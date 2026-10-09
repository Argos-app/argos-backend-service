package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {
    List<Property> findByUserIdAndActiveTrue(UUID userId);

    List<Property> findByActiveOrderByNameAsc(boolean active);

    Page<Property> findByActive(Pageable pageable, boolean active);
}
