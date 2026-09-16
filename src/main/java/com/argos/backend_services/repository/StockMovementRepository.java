package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Application;
import com.argos.backend_services.entity.MedicineBatch;
import com.argos.backend_services.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    List<StockMovement> findByMedicineBatchId(MedicineBatch id);

    List<StockMovement> findByApplicationId(Application id);
}
