package com.argos.backend_services.mapper;

import com.argos.backend_services.dto.out.ApplicationWithdrawalOut;
import com.argos.backend_services.entity.Application;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;

@Mapper(componentModel = "spring")
public interface ApplicationWithdrawalMapper {

    @Mapping(source = "id", target = "applicationId")
    @Mapping(source = "userId.id", target = "userId")
    @Mapping(source = "lotId.lotCode", target = "lotName")
    @Mapping(source = "medicineBatchId.medicineId.brandName", target = "medicineName")
    @Mapping(target = "daysFromReferenceDate", expression = "java(daysFromReferenceDate(application.getMeatWithdrawalEnd(), referenceDate))")
    ApplicationWithdrawalOut toOut(Application application, LocalDate referenceDate);

    default long daysFromReferenceDate(LocalDate withdrawalDate, LocalDate referenceDate) {
        return withdrawalDate.toEpochDay() - referenceDate.toEpochDay();
    }
}
