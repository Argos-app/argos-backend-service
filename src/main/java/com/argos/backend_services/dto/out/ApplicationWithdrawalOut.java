package com.argos.backend_services.dto.out;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class ApplicationWithdrawalOut {
    private UUID applicationId;
    private UUID userId;
    private String lotName;
    private String medicineName;
    private LocalDate applicationDate;
    private LocalDate meatWithdrawalEnd;
    private long daysFromReferenceDate;
    private long daysUntilSlaughter;
}
