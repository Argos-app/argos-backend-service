package com.argos.backend_services.dto.out;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class AnimalApplicationOut {
    private String medicineName;
    private String earCode;
    private LocalDate applicationDate;
    private String lotCode;
    private LocalDate meatWithdrawalEnd;
    private String status;
    private long daysFromReferenceDate;
    private long daysUntilSlaughter;
}
