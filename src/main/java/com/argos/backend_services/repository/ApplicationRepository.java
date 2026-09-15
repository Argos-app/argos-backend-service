package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    @Procedure(procedureName = "sp_registrar_aplicacao")
    void spRegistrarAplicacao (@Param("lot_id") UUID lotId,
                               @Param("medicine_batch_id") UUID medicineBatchID,
                               @Param("user_id") UUID user_id,
                               @Param("dose") BigDecimal dose,
                               @Param("unit") String unit,
                               @Param("route") String route,
                               @Param("goal") String goal);

    List<Application> findByLotId(UUID lotId);
}
