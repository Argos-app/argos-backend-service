package com.argos.backend_services.dto.out;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class HomeApplicationOut {
    private UUID applicationId;
    private UUID userId;
    private List<LotApplicationOut> lotApplication;
    private List<AnimalApplicationOut> animalApplication;
}
