package com.argos.backend_services.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Item paginado do estoque de medicamentos disponíveis")
public record MedicineListItemOut(
        @Schema(description = "Identificador do lote de medicamento", format = "uuid") UUID medicineBatchId,
        @Schema(description = "Nome comercial do medicamento", example = "Ivermectina") String medicineName,
        @Schema(description = "Fornecedor responsável pelo lote", example = "Distribuidora Veterinária Ltda") String supplier,
        @Schema(description = "Data de fabricação", example = "2026-01-10") LocalDate manufactureDate,
        @Schema(description = "Data de validade", example = "2028-01-10") LocalDate expiryDate,
        @Schema(description = "Saldo atual disponível no lote", example = "500.000") BigDecimal quantity,
        @Schema(description = "Dose cadastrada para o medicamento", example = "1.000") BigDecimal dose,
        @Schema(description = "Tempo de carência em dias", example = "28") Integer withdrawalDays,
        @Schema(description = "Número do lote", example = "LOTE-2026-001") String batch,
        @Schema(description = "Princípio ativo", example = "Ivermectina") String activeIngredient,
        @Schema(description = "Categoria terapêutica", example = "Antiparasitário") String therapeuticCategory
) {
}
