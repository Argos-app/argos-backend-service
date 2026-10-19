package com.argos.backend_services.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record HerdOut(
      UUID propertyId,
      String herdName,
      String breed,
      Integer headCount
) {
}
