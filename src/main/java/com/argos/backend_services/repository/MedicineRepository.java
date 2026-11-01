package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Medicine;
import com.argos.backend_services.entity.TherapeuticCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicineRepository extends JpaRepository<Medicine, UUID> {
    Page<Medicine> findByActiveTrue(Pageable pageable);
    List<Medicine> findByTherapeuticCategoryId(TherapeuticCategory therapeuticCategoryId);
    Optional<Medicine> findByBrandName(String brandName);
    Optional<Medicine> findByMapaRegistration(String mapaRegistration);
}
