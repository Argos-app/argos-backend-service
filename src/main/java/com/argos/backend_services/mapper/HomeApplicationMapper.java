package com.argos.backend_services.mapper;

import com.argos.backend_services.dto.out.AnimalApplicationOut;
import com.argos.backend_services.dto.out.LotApplicationOut;
import com.argos.backend_services.entity.Application;
import com.argos.backend_services.entity.ApplicationAnimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;

@Mapper(componentModel = "spring")
public interface HomeApplicationMapper {

    @Mapping(source = "application.medicineBatchId.medicineId.brandName", target = "medicineName")
    @Mapping(source = "application.applicationDate", target = "applicationDate")
    @Mapping(source = "application.meatWithdrawalEnd", target = "meatWithdrawalEnd")
    @Mapping(target = "daysFromReferenceDate", expression = "java(daysBetween(application.getMeatWithdrawalEnd(), referenceDate))")
    @Mapping(target = "daysUntilSlaughter", expression = "java(daysUntilSlaughter(application.getLotId().getSlaughterReleaseDate(), referenceDate, fitForSlaughter))")
    @Mapping(source = "application.lotId.lotCode", target = "lotCode")
    @Mapping(source = "application.headCountApplied", target = "headCount")
    @Mapping(target = "status", expression = "java(status(application.getLotId().getSlaughterReleaseDate(), referenceDate, fitForSlaughter))")
    LotApplicationOut toLotApplicationOut(Application application, LocalDate referenceDate, boolean fitForSlaughter);

    @Mapping(source = "application.medicineBatchId.medicineId.brandName", target = "medicineName")
    @Mapping(source = "application.applicationDate", target = "applicationDate")
    @Mapping(source = "applicationAnimal.meatWithdrawalDate", target = "meatWithdrawalEnd")
    @Mapping(target = "daysFromReferenceDate", expression = "java(daysBetween(applicationAnimal.getMeatWithdrawalDate(), referenceDate))")
    @Mapping(target = "daysUntilSlaughter", expression = "java(daysUntilSlaughter(applicationAnimal.getAnimalExceptionId().getSlaughterReleaseDate(), referenceDate, applicationAnimal.isReleasedForSlaughter()))")
    @Mapping(source = "applicationAnimal.animalExceptionId.earTag", target = "earCode")
    @Mapping(source = "applicationAnimal.animalExceptionId.lotId.lotCode", target = "lotCode")
    @Mapping(target = "status", expression = "java(status(applicationAnimal.getAnimalExceptionId().getSlaughterReleaseDate(), referenceDate, applicationAnimal.isReleasedForSlaughter()))")
    AnimalApplicationOut toAnimalApplicationOut(ApplicationAnimal applicationAnimal, Application application, LocalDate referenceDate);

    default long daysBetween(LocalDate date, LocalDate referenceDate) {
        return date == null ? 0 : date.toEpochDay() - referenceDate.toEpochDay();
    }

    default long daysUntilSlaughter(LocalDate slaughterDate, LocalDate referenceDate, boolean fitForSlaughter) {
        return fitForSlaughter || slaughterDate == null ? 0 : daysBetween(slaughterDate, referenceDate);
    }

    default String status(LocalDate slaughterDate, LocalDate referenceDate, boolean released) {
        return released || (slaughterDate != null && !slaughterDate.isAfter(referenceDate))
                ? "Concluida" : "Pendente";
    }
}
