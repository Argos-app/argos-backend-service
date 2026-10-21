package com.argos.backend_services.dto.out;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class LotApplicationOut {
    private String medicineName;
    private String lotCode;
    private LocalDate applicationDate;
    private LocalDate meatWithdrawalEnd;
    private String status;
    private Integer headCount;
    private long daysFromReferenceDate;
    private long daysUntilSlaughter;
}
