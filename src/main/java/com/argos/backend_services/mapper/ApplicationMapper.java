package com.argos.backend_services.mapper;

import com.argos.backend_services.dto.out.ApplicationOut;
import com.argos.backend_services.entity.Application;
import com.argos.backend_services.entity.ApplicationAnimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(source = "lotId.lotCode", target = "lotName")
    @Mapping(source = "medicineBatchId.medicineId.brandName", target = "medicineName")
    List<ApplicationOut> listToOut(List<Application> application);

    @Mapping(source = "lotId.lotCode", target = "lotName")
    @Mapping(source = "lotId.lotCode", target = "lotCode")
    @Mapping(source = "medicineBatchId.medicineId.brandName", target = "medicineName")
    @Mapping(source = "headCountApplied", target = "headCount")
    @Mapping(target = "status", expression = "java(application.getLotId().getSlaughterReleaseDate() != null && !application.getLotId().getSlaughterReleaseDate().isAfter(java.time.LocalDate.now()) ? \"Concluida\" : \"Pendente\")")
    ApplicationOut toOut(Application application);

    @Mapping(source = "application.lotId.lotCode", target = "lotName")
    @Mapping(source = "application.lotId.lotCode", target = "lotCode")
    @Mapping(source = "application.medicineBatchId.medicineId.brandName", target = "medicineName")
    @Mapping(source = "applicationAnimal.animalExceptionId.earTag", target = "earCode")
    @Mapping(source = "applicationAnimal.meatWithdrawalDate", target = "meatWithdrawalEnd")
    @Mapping(target = "headCount", constant = "1")
    @Mapping(target = "status", expression = "java(applicationAnimal.isReleasedForSlaughter() || (applicationAnimal.getAnimalExceptionId().getSlaughterReleaseDate() != null && !applicationAnimal.getAnimalExceptionId().getSlaughterReleaseDate().isAfter(java.time.LocalDate.now())) ? \"Concluida\" : \"Pendente\")")
    ApplicationOut toOut(ApplicationAnimal applicationAnimal, Application application);

    Application toEntity(ApplicationOut applicationOut);
}
