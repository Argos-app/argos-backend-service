package com.argos.backend_services.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Dados para registrar a entrada de um lote de medicamento cadastrado no MAPA")
public record MedicineBatchIn(
        @NotBlank
        @Schema(description = "Registro MAPA do produto usado para localizar o medicamento", example = "12345")
        String productRegistration,

        @NotBlank
        @Schema(description = "Número identificador do lote recebido", example = "LOTE-2026-001")
        String batchNumber,

        @NotNull
        @Schema(description = "Data de fabricação", example = "2026-01-10")
        LocalDate manufactureDate,

        @NotNull
        @Schema(description = "Data de validade", example = "2028-01-10")
        LocalDate expiryDate,

        @NotNull
        @Positive
        @Schema(description = "Quantidade recebida no lote", example = "500.000")
        BigDecimal quantity,

        @NotBlank
        @Schema(description = "Razão social ou nome do fornecedor", example = "Distribuidora Veterinária Ltda")
        String supplierName,

        @NotBlank
        @Pattern(regexp = "[0-9]{14}", message = "CNPJ deve conter 14 dígitos numéricos")
        @Schema(description = "CNPJ do fornecedor, com 14 dígitos e sem pontuação", example = "12345678000199")
        String supplierCnpj,

        @NotNull
        @Schema(description = "Propriedade que receberá o estoque", format = "uuid")
        UUID propertyId
) {
}
