package com.argos.backend_services.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@Schema(description = "Informações para cadastro de lote")
public record LotIn(

        @NotBlank
        @Schema(description = "nome do lote", example = "Lote Engorda")
        String lotName,

        @NotBlank
        @Schema(description = "nome do rebanho", example = "Rebanho apto a abate")
        String herdName,

        @NotBlank
        @Schema(description = "Categoria do lote", example = "Engorda/Recria/Apto Abate")
        String category,

        @NotNull
        @Schema(description = "Quantidade de bois do lote", example = "200")
        Integer headCount,

        @NotNull
        @Schema(description = "Dia previsto pra abate", example = "2026/07/01")
        LocalDate releaseSlaughterDate
) {
}
