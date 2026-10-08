package com.argos.backend_services.repository;

import com.argos.backend_services.entity.UserProperty;
import com.argos.backend_services.entity.UserPropertyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPropertyRepository extends JpaRepository<UserProperty, UserPropertyId> {}
