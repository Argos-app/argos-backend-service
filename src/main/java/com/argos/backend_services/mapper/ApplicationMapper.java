package com.argos.backend_services.mapper;

import com.argos.backend_services.dto.out.ApplicationOut;
import com.argos.backend_services.entity.Application;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    ApplicationOut toOut(Application application);
}
