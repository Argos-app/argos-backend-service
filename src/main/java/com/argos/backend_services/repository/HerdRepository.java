package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HerdRepository extends JpaRepository<Herd, UUID> {
    List<Herd> findByPropertyId(Property propertyId);
}
