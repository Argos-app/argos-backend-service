package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    List<Permission> findByActiveTrueOrderByNameAsc();
}
