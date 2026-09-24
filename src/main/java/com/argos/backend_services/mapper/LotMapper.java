package com.argos.backend_services.mapper;

import com.argos.backend_services.dto.in.LotIn;
import com.argos.backend_services.dto.out.LotOut;
import com.argos.backend_services.entity.Lot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LotMapper {
    @Mapping(source = "lotCode", target = "lotName")
    @Mapping(source = "openDate", target = "openDAte")
    @Mapping(source = "slaughterReleaseDate", target = "releaseSlaughterDate")
    LotOut toOut(Lot lot);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "herdId", ignore = true)
    @Mapping(source = "lotName", target = "lotCode")
    @Mapping(source = "releaseSlaughterDate", target = "slaughterReleaseDate")
    @Mapping(target = "parentId", ignore = true)
    @Mapping(target = "isExceptionLot", constant = "false")
    @Mapping(target = "fitForSlaughter", constant = "false")
    @Mapping(target = "openDate", ignore = true)
    @Mapping(target = "closeDate", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Lot toEntity(LotIn input);
}
