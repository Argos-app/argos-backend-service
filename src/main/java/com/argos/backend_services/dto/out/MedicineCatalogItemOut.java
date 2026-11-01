package com.argos.backend_services.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record MedicineCatalogItemOut(
        UUID medicineId,
        String medicineName,
        @Schema(description = "Registro MAPA do medicamento, recebido como productRegistration no cadastro de lote")
        String productRegistration
) {
}
