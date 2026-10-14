package com.argos.backend_services.dto.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ApplicationHistoryOut(
        UUID id,
        String lotCode,
        String lotCategory,
        String propertyName,
        String appliedByName,
        LocalDate applicationDate,
        LocalTime applicationTime,
        BigDecimal dose,
        String unit,
        String medicineName,
        String medicineBatchNumber,
        String route,
        LocalDate meatWithdrawalEnd,
        String goal,
        String notes
) {
}
