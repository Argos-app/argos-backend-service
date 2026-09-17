package com.argos.backend_services.mapper;

import com.argos.backend_services.dto.out.ApplicationOut;
import com.argos.backend_services.entity.Application;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(source = "lotId.lotCode", target = "lotName")
    @Mapping(source = "medicineBatchId.medicineId.brandName", target = "medicineName")
    List<ApplicationOut> listToOut(List<Application> application);

    @Mapping(source = "lotId.lotCode", target = "lotName")
    @Mapping(source = "medicineBatchId.medicineId.brandName", target = "medicineName")
    ApplicationOut toOut(Application application);
    Application toEntity(ApplicationOut applicationOut);
}
