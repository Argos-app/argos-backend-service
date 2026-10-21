package com.argos.backend_services.dto.out;

import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Builder
@Schema(description = "Dados de visualização de um animal em exceção")
public record AnimalExceptionOut(
        @Schema(description = "Identificador da exceção") UUID id,
        @Schema(description = "Número do brinco", example = "BR-0001") String earTag,
        @Schema(description = "Código do lote de exceção atual", example = "LOTE-01-EXC1") String currentLotCode,
        @Schema(description = "Código do lote de origem", example = "LOTE-01") String originLotCode,
        @Schema(description = "Data de registro da exceção", example = "2026-10-09") LocalDate exceptionDate,
        @Schema(description = "Data de liberação para abate", example = "2026-10-16") LocalDate slaughterReleaseDate,
        @Schema(description = "Motivo da exceção", example = "Doença respiratória") String exceptionReason,
        @Schema(description = "Status do animal", example = "ATIVO") String status,
        @Schema(description = "Indica aptidão para abate") boolean fitForSlaughter,
        @Schema(description = "Indica aptidão para retorno ao lote de origem") boolean eligibleToReturn,
        @Schema(description = "Percentual de aptidão para retorno", example = "100") int returnFitnessPercentage,
        @Schema(description = "Tag de aptidão", example = "APTO") String eligibilityTag,
        @Schema(description = "Último medicamento aplicado", example = "Ivermectina") String latestMedicine,
        @Schema(description = "Fim da carência da última aplicação", example = "2026-10-16") LocalDate latestMedicineWithdrawalEnd
) {
}
