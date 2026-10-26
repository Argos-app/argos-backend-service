package com.argos.backend_services.mapper;

import com.argos.backend_services.dto.out.ApplicationWithdrawalOut;
import com.argos.backend_services.entity.Application;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;

@Mapper(componentModel = "spring")
public interface ApplicationWithdrawalMapper {

    @Mapping(source = "application.id", target = "applicationId")
    @Mapping(source = "application.lotId.id", target = "lotId")
    @Mapping(source = "application.userId.id", target = "userId")
    @Mapping(source = "application.lotId.lotCode", target = "lotName")
    @Mapping(source = "application.medicineBatchId.medicineId.brandName", target = "medicineName")
    @Mapping(source = "application.applicationDate", target = "applicationDate")
    @Mapping(target = "daysFromReferenceDate", expression = "java(daysFromReferenceDate(application.getMeatWithdrawalEnd(), referenceDate))")
    @Mapping(target = "daysUntilSlaughter", expression = "java(daysUntilSlaughter(application.getLotId().getSlaughterReleaseDate(), referenceDate))")
    ApplicationWithdrawalOut toOut(Application application, LocalDate referenceDate);

    default long daysFromReferenceDate(LocalDate withdrawalDate, LocalDate referenceDate) {
        return withdrawalDate.toEpochDay() - referenceDate.toEpochDay();
    }

    default long daysUntilSlaughter(LocalDate slaughterDate, LocalDate referenceDate) {
        return slaughterDate == null ? 0 : slaughterDate.toEpochDay() - referenceDate.toEpochDay();
    }
}
