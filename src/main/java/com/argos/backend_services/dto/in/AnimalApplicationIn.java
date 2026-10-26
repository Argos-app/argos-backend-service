package com.argos.backend_services.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Dados para aplicação individual de medicamento em um animal de exceção")
public record AnimalApplicationIn(
        @NotBlank
        @Schema(description = "Número do brinco do animal", example = "BR-0001")
        String earCode,

        @NotNull
        @Schema(description = "Identificador do lote do medicamento", example = "3d2135ac-f45b-4b46-aa7c-61ac0e6c3c7e")
        UUID medicineBatchId,

        @NotNull
        @Positive
        @Schema(description = "Dose aplicada", example = "5.00")
        BigDecimal dose,

        @NotBlank
        @Schema(description = "Unidade da dose", example = "ml")
        String unit,

        @NotBlank
        @Schema(description = "Via de aplicação", example = "ORAL")
        String route,

        @Schema(description = "Finalidade da aplicação", example = "Tratamento")
        String goal
) {
}
