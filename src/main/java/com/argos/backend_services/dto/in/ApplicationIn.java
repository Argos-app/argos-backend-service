package com.argos.backend_services.dto.in;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record ApplicationIn(
        UUID medicineBatchId,
        UUID lotId,
        String lotName,
        String medicineName,
        LocalDate applicationDate,
        BigDecimal dose,
        String unit,
        String route,
        String goal
)
{}
