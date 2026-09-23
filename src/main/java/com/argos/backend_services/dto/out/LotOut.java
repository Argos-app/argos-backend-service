package com.argos.backend_services.dto.out;

import java.time.LocalDate;
import java.util.List;

public class LotOut {
    private String lotName;
    private Integer headCount;
    private LocalDate openDate;
    private LocalDate slaughterReleaseDate;
    private String category;
    private List<AnimalExceptionOut> animalExceptionOutList;

    public static class AnimalExceptionOut {
        private String earCode;
        private String medicine;
        private LocalDate meatWithdrawalEndDate;
    }
}
