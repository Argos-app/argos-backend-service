package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Medicine;
import com.argos.backend_services.entity.MedicineBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

public interface MedicineBatchRepository extends JpaRepository<MedicineBatch, UUID> {

    Optional<MedicineBatch> findByMedicineId(Medicine id);

    boolean existsByMedicineIdAndBatchNumber(Medicine medicine, String batchNumber);

    @EntityGraph(attributePaths = {"medicineId", "medicineId.therapeuticCategoryId", "medicineId.supplierId"})
    Page<MedicineBatch> findByStatus(MedicineBatch.Status status, Pageable pageable);

    @EntityGraph(attributePaths = {"medicineId", "medicineId.therapeuticCategoryId", "medicineId.supplierId"})
    Page<MedicineBatch> findByStatusAndMedicineId_TherapeuticCategoryId(
            MedicineBatch.Status status,
            UUID therapeuticCategoryId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"medicineId", "medicineId.therapeuticCategoryId", "medicineId.supplierId"})
    @Query(value = """
            select distinct mb from MedicineBatch mb
            join Application a on a.medicineBatchId = mb
            where a.lotId.herdId.propertyId.id = :propertyId
              and mb.status = :status
              and (:categoryId is null or mb.medicineId.therapeuticCategoryId.id = :categoryId)
            """, countQuery = """
            select count(distinct mb.id) from MedicineBatch mb
            join Application a on a.medicineBatchId = mb
            where a.lotId.herdId.propertyId.id = :propertyId
              and mb.status = :status
              and (:categoryId is null or mb.medicineId.therapeuticCategoryId.id = :categoryId)
            """)
    Page<MedicineBatch> findAppliedInProperty(
            @Param("propertyId") UUID propertyId,
            @Param("status") MedicineBatch.Status status,
            @Param("categoryId") UUID categoryId,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MedicineBatch mb
            set mb.status = com.argos.backend_services.entity.MedicineBatch.Status.VENCIDO
            where mb.status = com.argos.backend_services.entity.MedicineBatch.Status.DISPONIVEL
              and mb.expiryDate < :referenceDate
            """)
    int markAvailableBatchesExpired(@Param("referenceDate") LocalDate referenceDate);
}
