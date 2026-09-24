package com.argos.backend_services.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO de saída do lote")
public class LotOut {

    private String lotName;
    private LocalDate openDAte;
    private LocalDate releaseSlaughterDate;
    private String category;
    private List<AnimalExceptionOut> animalExceptionOutList;

    @Getter
    @Setter
    @Builder
    @Schema(description = "Dados de animal de lote de exceção")
    public static class AnimalExceptionOut {
        @Schema(description = "Brinco do animal", example = "BR-0001")
        private String earCode;
        @Schema(description = "Medicamento aplicado", example = "Ivermectina")
        private String medicine;
        @Schema(description = "Data de liberação para abate", example = "2026-07-01")
        private String releaseSlaughterDate;
    }
}
