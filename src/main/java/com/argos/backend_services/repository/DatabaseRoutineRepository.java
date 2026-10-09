package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Application;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public interface DatabaseRoutineRepository extends Repository<Application, UUID> {

    @Procedure(procedureName = "sp_registrar_aplicacao")
    void registerApplication(
            @Param("p_lot_id") UUID lotId,
            @Param("p_medicine_batch_id") UUID medicineBatchId,
            @Param("p_user_id") UUID userId,
            @Param("p_dose") BigDecimal dose,
            @Param("p_unit") String unit,
            @Param("p_route") String route,
            @Param("p_goal") String goal
    );

    @Procedure(procedureName = "sp_marcar_excecao")
    void registerException(
            @Param("p_lot_id") UUID lotId,
            @Param("p_ear_tag") String earTag,
            @Param("p_motivo") String motivo,
            @Param("p_peso") BigDecimal peso
    );

    @Procedure(procedureName = "sp_registrar_aplicacao_animal")
    void registerAnimalApplication(
            @Param("p_animal_exception_id") UUID animalExceptionId,
            @Param("p_medicine_batch_id") UUID medicineBatchId,
            @Param("p_user_id") UUID userId,
            @Param("p_dose") BigDecimal dose,
            @Param("p_unit") String unit,
            @Param("p_route") String route,
            @Param("p_goal") String goal
    );

    @Procedure(procedureName = "sp_retornar_excecao")
    void returnException(@Param("p_animal_id") UUID animalExceptionId);

    @Query(value = "select fn_data_fim_carencia(:medicineId, :date)", nativeQuery = true)
    LocalDate calculateSlaughterDate(
            @Param("medicineId") UUID medicineId,
            @Param("date") LocalDate date
    );

    @Query(value = "select fn_lote_apto_abate(:lotId, :date)", nativeQuery = true)
    Boolean lotIsReleaseForSlaughter(
            @Param("lotId") UUID lotId,
            @Param("date") LocalDate date
    );

    @Query(value = "select fn_saldo_medicine_batch(:batchId)", nativeQuery = true)
    BigDecimal balanceMedicineBatch(@Param("batchId") UUID batchId);
}
