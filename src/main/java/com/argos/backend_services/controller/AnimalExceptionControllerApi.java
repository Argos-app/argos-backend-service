package com.argos.backend_services.controller;

import com.argos.backend_services.controller.annotation.CurrentUser;

import com.argos.backend_services.dto.in.AnimalExceptionIn;
import com.argos.backend_services.dto.out.AnimalExceptionOut;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.security.CurrentUserData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@RequestMapping("/animal-exceptions")
@Tag(name = "Exceções animais", description = "Registro e acompanhamento de animais em lote de exceção")
public interface AnimalExceptionControllerApi {

    @PostMapping
    @Operation(summary = "Registra uma exceção animal")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Exceção registrada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou lote não disponível", content = @Content),
            @ApiResponse(responseCode = "404", description = "Lote de origem não encontrado", content = @Content)
    })
    ResponseEntity<Void> create(
            @Valid @RequestBody AnimalExceptionIn input,
            @Parameter(hidden = true) @CurrentUser CurrentUserData currentUser) throws LotNotFoundException;

    @GetMapping
    @Operation(summary = "Lista animais em exceção", description = "Os filtros de atividade e lote de origem são opcionais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exceções encontradas",
                    content = @Content(schema = @Schema(implementation = PageOut.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos", content = @Content)
    })
    ResponseEntity<PageOut<AnimalExceptionOut>> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) UUID originId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(hidden = true) @CurrentUser CurrentUserData currentUser);
}
