package com.argos.backend_services.controller;

import com.argos.backend_services.dto.in.AnimalApplicationIn;
import com.argos.backend_services.exception.MedicineNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@RequestMapping("/animal-application")
@Tag(name = "Aplicações em animais", description = "Aplicação individual de medicamentos em animais de exceção")
public interface AnimalApplicationControllerApi {

    @PostMapping("/{animalExceptionId}")
    @Operation(summary = "Registra aplicação individual", description = "Retorna o animal ao lote de origem somente quando a carência acabou e o lote continua ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Aplicação registrada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou animal sem condição para aplicação", content = @Content),
            @ApiResponse(responseCode = "404", description = "Animal ou lote de medicamento não encontrado", content = @Content)
    })
    ResponseEntity<Void> create(
            @PathVariable UUID animalExceptionId,
            @Valid @RequestBody AnimalApplicationIn input,
            @Parameter(description = "Identificador do usuário", required = true)
            @RequestHeader UUID userId) throws MedicineNotFoundException;
}
