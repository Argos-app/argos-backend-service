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

    @Schema(description = "Nome do lote")
    private String lotName;
    @Schema(description = "Data de criação do lote")
    private LocalDate openDate;
    @Schema(description = "Dia estimado de abate")
    private LocalDate releaseSlaughterDate;
    @Schema(description = "Categoria do lote: ENGORDA/RECRIA")
    private String category;
    @Schema(description = "em caso de ser lote de exceção: lista de animais e o dia estimado de abate")
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
        private LocalDate releaseSlaughterDate;
    }
}
