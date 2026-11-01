package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, UUID> {
    @Query("""
            select p from UserProperty up join up.property p
            where up.user.id = :userId and up.active = true and p.active = true
            order by lower(p.name) asc, p.id asc
            """)
    List<Property> findActiveLinkedProperties(@Param("userId") UUID userId, Pageable pageable);

    List<Property> findByUserIdAndActiveTrue(UUID userId);

    List<Property> findByUser_IdAndActiveTrueOrderByNameAsc(UUID administratorId);

    Page<Property> findByUser_IdAndActiveTrue(UUID administratorId, Pageable pageable);

    Optional<Property> findByIdAndUser_Id(UUID propertyId, UUID administratorId);

    boolean existsByIdAndUser_IdAndActiveTrue(UUID propertyId, UUID userId);

    boolean existsByUser_Id(UUID userId);
}
