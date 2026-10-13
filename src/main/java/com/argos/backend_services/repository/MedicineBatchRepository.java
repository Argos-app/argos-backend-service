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

    Optional<MedicineBatch> findByMedicineIdAndProperty_Id(Medicine id, UUID propertyId);

    boolean existsByMedicineIdAndBatchNumberAndProperty_Id(Medicine medicine, String batchNumber, UUID propertyId);

    @EntityGraph(attributePaths = {"medicineId", "medicineId.therapeuticCategoryId", "supplierId"})
    Page<MedicineBatch> findByStatusAndProperty_IdIn(MedicineBatch.Status status, Collection<UUID> propertyIds, Pageable pageable);

    @EntityGraph(attributePaths = {"medicineId", "medicineId.therapeuticCategoryId", "supplierId"})
    Page<MedicineBatch> findByStatusAndMedicineId_TherapeuticCategoryIdAndProperty_IdIn(
            MedicineBatch.Status status,
            UUID therapeuticCategoryId,
            Collection<UUID> propertyIds,
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
