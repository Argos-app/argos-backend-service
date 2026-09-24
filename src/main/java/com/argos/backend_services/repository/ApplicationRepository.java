package com.argos.backend_services.repository;

import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Application;
import com.argos.backend_services.entity.Lot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    List<Application> findByLotId(Lot lotId);
    List<Application> findByLotId_LotCode(String lotCode);
    List<Application> findByUserId(AppUser userId);

    List<Application> findByMeatWithdrawalEndBetween(LocalDate startDate, LocalDate endDate);

    @EntityGraph(attributePaths = {"userId", "lotId", "medicineBatchId", "medicineBatchId.medicineId"})
    List<Application> findByUserId_IdAndMeatWithdrawalEndBetween(UUID userId, LocalDate startDate, LocalDate endDate);
}
