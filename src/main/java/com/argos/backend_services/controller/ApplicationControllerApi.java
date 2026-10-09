package com.argos.backend_services.controller;

import com.argos.backend_services.dto.in.ApplicationIn;
import com.argos.backend_services.dto.out.ApplicationOut;
import com.argos.backend_services.dto.out.ApplicationWithdrawalOut;
import com.argos.backend_services.exception.InvalidDateWindowException;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.exception.MedicineNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RequestMapping("/application")
@Tag(name = "Applications", description = "Gerenciamento e consulta de aplicações")
public interface ApplicationControllerApi {

    @PostMapping
    @Operation(summary = "Registra uma aplicação", description = "Vincula a aplicação ao usuário informado e valida o lote e o medicamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aplicação registrada"),
            @ApiResponse(responseCode = "400", description = "Dados da aplicação inválidos"),
            @ApiResponse(responseCode = "422", description = "Lote ou medicamento não encontrado")
    })
    ResponseEntity<Void> createApplication(@Parameter(description = "Identificador do usuário") @RequestHeader UUID userId,
                                           @Valid @RequestBody ApplicationIn applicationIn) throws LotNotFoundException, MedicineNotFoundException;

    @GetMapping("/list-all")
    @Operation(summary = "Lista aplicações")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aplicações encontradas"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    })
    ResponseEntity<List<ApplicationOut>> listAll();

    @GetMapping("/withdrawal-window")
    @Operation(summary = "Lista aplicações próximas da janela de retirada", description = "Usa a data atual como referência quando nenhuma data é informada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aplicações encontradas"),
            @ApiResponse(responseCode = "400", description = "Janela de datas inválida")
    })
    ResponseEntity<List<ApplicationWithdrawalOut>> withdrawalWindow(
            @Parameter(description = "Data de referência no formato yyyy-MM-dd") @RequestParam(required = false) LocalDate referenceDate,
            @Parameter(description = "Dias anteriores à data de referência") @RequestParam(defaultValue = "7") int daysBefore,
            @Parameter(description = "Dias posteriores à data de referência") @RequestParam(defaultValue = "7") int daysAfter) throws InvalidDateWindowException;
}
