package com.argos.backend_services.repository;

import com.argos.backend_services.entity.UserProperty;
import com.argos.backend_services.entity.UserPropertyId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserPropertyRepository extends JpaRepository<UserProperty, UserPropertyId> {
    List<UserProperty> findAllByProperty_IdAndActiveTrue(UUID propertyId);
}
