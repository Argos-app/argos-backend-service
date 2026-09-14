package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MedicineRepository extends JpaRepository<Medicine, UUID> {
    List<Medicine> findBytherapeuticCategoryId(UUID therapeuticCategoryId);
}
