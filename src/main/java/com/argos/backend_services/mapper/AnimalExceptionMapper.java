package com.argos.backend_services.mapper;

import com.argos.backend_services.dto.out.AnimalExceptionOut;
import com.argos.backend_services.entity.AnimalException;
import com.argos.backend_services.entity.ApplicationAnimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring")
public interface AnimalExceptionMapper {

    @Mapping(source = "animalException.id", target = "id")
    @Mapping(source = "animalException.lotId.lotCode", target = "currentLotCode")
    @Mapping(source = "animalException.originLotId.lotCode", target = "originLotCode")
    @Mapping(source = "latestApplication.applicationId.medicineBatchId.medicineId.brandName", target = "latestMedicine")
    @Mapping(source = "latestApplication.meatWithdrawalDate", target = "latestMedicineWithdrawalEnd")
    @Mapping(target = "eligibleToReturn", expression = "java(eligibleToReturn(animalException))")
    @Mapping(target = "returnFitnessPercentage", expression = "java(eligibleToReturn(animalException) ? 100 : 0)")
    @Mapping(target = "eligibilityTag", expression = "java(eligibleToReturn(animalException) ? \"APTO\" : \"INAPTO\")")
    AnimalExceptionOut toOut(AnimalException animalException, ApplicationAnimal latestApplication);

    default ApplicationAnimal lastApplication(
            AnimalException animalException,
            List<ApplicationAnimal> animalApplications) {
        return animalApplications.stream()
                .filter(application -> application.getAnimalExceptionId().getId().equals(animalException.getId()))
                .max(Comparator.comparing(ApplicationAnimal::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    default boolean eligibleToReturn(AnimalException animalException) {
        return animalException.getStatus() == AnimalException.StatusAnimal.ATIVO
                && animalException.isFitForSlaughter()
                && animalException.getOriginLotId().isActive();
    }
}
