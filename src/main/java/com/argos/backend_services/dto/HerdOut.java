package com.argos.backend_services.dto;

import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Builder
public record HerdOut(
      @Schema(description = "UUID do rebanho") UUID herdId,
      UUID propertyId,
      String herdName,
      String breed,
      Integer headCount
) {
}
