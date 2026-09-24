package com.argos.backend_services.dto.out;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
@Getter
@Setter
public class ApplicationOut{
        String lotName;
        String lotCode;
        String earCode;
        String medicineName;
        LocalDateTime applicationDate;
        LocalTime applicationTime;
        BigDecimal dose;
        String unit;
        String route;
        Integer headCountApplied;
        Integer headCount;
        LocalDate meatWithdrawalEnd;
        String status;
        String goal;
        String notes;
}
