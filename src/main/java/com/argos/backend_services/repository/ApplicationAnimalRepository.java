package com.argos.backend_services.repository;

import com.argos.backend_services.entity.AnimalException;
import com.argos.backend_services.entity.ApplicationAnimal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationAnimalRepository extends JpaRepository<ApplicationAnimal, UUID> {

    List<ApplicationAnimal> findByAnimalExceptionId (AnimalException animalExceptionId);
}
