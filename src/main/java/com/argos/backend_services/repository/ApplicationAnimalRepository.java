package com.argos.backend_services.repository;

import com.argos.backend_services.entity.AnimalException;
import com.argos.backend_services.entity.ApplicationAnimal;
import com.argos.backend_services.entity.Application;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationAnimalRepository extends JpaRepository<ApplicationAnimal, UUID> {

    @EntityGraph(attributePaths = {
            "applicationId", "applicationId.medicineBatchId", "applicationId.medicineBatchId.medicineId"
    })
    List<ApplicationAnimal> findByAnimalExceptionId(AnimalException animalExceptionId);

    @EntityGraph(attributePaths = {
            "applicationId", "applicationId.medicineBatchId", "applicationId.medicineBatchId.medicineId"
    })
    List<ApplicationAnimal> findByAnimalExceptionIdIn(List<AnimalException> animalExceptions);

    @EntityGraph(attributePaths = "animalExceptionId")
    List<ApplicationAnimal> findByApplicationId(Application applicationId);

}
