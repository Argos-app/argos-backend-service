package com.argos.backend_services.repository;

import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Application;
import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.dto.out.ApplicationHistoryOut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    @Query(value = """
            select new com.argos.backend_services.dto.out.ApplicationHistoryOut(
                a.id,
                lot.lotCode,
                lot.category,
                property.name,
                appliedBy.name,
                a.applicationDate,
                a.applicationTime,
                a.dose,
                a.unit,
                medicine.brandName,
                medicineBatch.batchNumber,
                a.route,
                a.meatWithdrawalEnd,
                a.goal,
                a.notes
            )
            from Application a
            join a.lotId lot
            join lot.herdId herd
            join herd.propertyId property
            join a.userId appliedBy
            join a.medicineBatchId medicineBatch
            join medicineBatch.medicineId medicine
            where property.user.id = :administratorId
            order by a.applicationDate desc,
                case when a.applicationTime is null then 1 else 0 end,
                a.applicationTime desc,
                a.id desc
            """,
            countQuery = """
            select count(a)
            from Application a
            join a.lotId lot
            join lot.herdId herd
            join herd.propertyId property
            where property.user.id = :administratorId
            """)
    Page<ApplicationHistoryOut> findHistoryByAdministratorId(
            @Param("administratorId") UUID administratorId,
            Pageable pageable
    );

    List<Application> findByLotId(Lot lotId);
    List<Application> findByLotId_LotCode(String lotCode);
    List<Application> findByUserId(AppUser userId);

    List<Application> findByMeatWithdrawalEndBetween(LocalDate startDate, LocalDate endDate);

    @EntityGraph(attributePaths = {"userId", "lotId", "medicineBatchId", "medicineBatchId.medicineId"})
    List<Application> findByUserId_IdAndMeatWithdrawalEndBetween(UUID userId, LocalDate startDate, LocalDate endDate);
}
