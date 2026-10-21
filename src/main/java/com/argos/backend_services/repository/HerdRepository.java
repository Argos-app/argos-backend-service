package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface HerdRepository extends JpaRepository<Herd, UUID> {
    List<Herd> findByPropertyId(Property propertyId);
    Optional<Herd> findByHerdNameAndActiveTrue(String herdName);

    @EntityGraph(attributePaths = "propertyId")
    Optional<Herd> findWithPropertyByHerdNameAndActiveTrue(String herdName);
}
