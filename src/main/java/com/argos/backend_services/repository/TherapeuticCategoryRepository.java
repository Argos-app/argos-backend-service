package com.argos.backend_services.repository;

import com.argos.backend_services.entity.TherapeuticCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TherapeuticCategoryRepository extends JpaRepository<TherapeuticCategory, UUID> {
}
