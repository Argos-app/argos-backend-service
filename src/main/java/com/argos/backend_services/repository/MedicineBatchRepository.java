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

    boolean existsByMedicineIdAndBatchNumberAndPropertyId_Id(
            Medicine medicine, String batchNumber, UUID propertyId);

    boolean existsByIdAndPropertyId_Id(UUID batchId, UUID propertyId);

    @EntityGraph(attributePaths = {"medicineId", "medicineId.therapeuticCategoryId", "medicineId.supplierId"})
    @Query(value = """
            select mb from MedicineBatch mb join mb.medicineId m
            where mb.propertyId.id = :propertyId
              and mb.status = :status
              and (:categoryId is null or m.therapeuticCategoryId.id = :categoryId)
            """, countQuery = """
            select count(mb) from MedicineBatch mb join mb.medicineId m
            where mb.propertyId.id = :propertyId
              and mb.status = :status
              and (:categoryId is null or m.therapeuticCategoryId.id = :categoryId)
            """)
    Page<MedicineBatch> findAvailableInProperty(
            @Param("propertyId") UUID propertyId,
            @Param("status") MedicineBatch.Status status,
            @Param("categoryId") UUID categoryId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"medicineId", "medicineId.therapeuticCategoryId", "medicineId.supplierId"})
    Page<MedicineBatch> findByStatus(MedicineBatch.Status status, Pageable pageable);

    @EntityGraph(attributePaths = {"medicineId", "medicineId.therapeuticCategoryId", "medicineId.supplierId"})
    Page<MedicineBatch> findByStatusAndMedicineId_TherapeuticCategoryId(
            MedicineBatch.Status status,
            UUID therapeuticCategoryId,
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
