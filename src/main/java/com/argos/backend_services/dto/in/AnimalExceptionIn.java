package com.argos.backend_services.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Dados necessários para registrar um animal em exceção")
public record AnimalExceptionIn(
        @NotNull @Schema(description = "Identificador do lote de origem", example = "7d2de32d-8198-4f71-a076-e5862378cb63") UUID originLotId,
        @NotBlank @Schema(description = "Número do brinco do animal", example = "BR-0001") String earTag,
        @NotBlank @Schema(description = "Motivo da exceção", example = "Doença respiratória") String exceptionReason,
        @Positive @Schema(description = "Peso atual do animal em quilogramas", example = "420.50") BigDecimal currentWeight
) {
}
