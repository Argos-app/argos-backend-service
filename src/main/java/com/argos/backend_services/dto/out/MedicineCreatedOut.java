package com.argos.backend_services.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Identificadores criados no registro de entrada de medicamento")
public record MedicineCreatedOut(
        @Schema(description = "Identificador do lote criado", format = "uuid") UUID medicineBatchId,
        @Schema(description = "Identificador do fornecedor associado ao lote", format = "uuid") UUID supplierId
) {
}
