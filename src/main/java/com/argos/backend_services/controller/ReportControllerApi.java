package com.argos.backend_services.controller;

import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.controller.handler.ApiError;
import com.argos.backend_services.security.CurrentUserData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface ReportControllerApi {

    @GetMapping("/herds/{reportId}/export")
    @Operation(summary = "Exporta um relatório de rebanho em XLSX",
            description = "Recebe o ID do documento MongoDB. Exclusivo para administradores da fazenda ativa do relatório.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo XLSX", content = @Content(
                    mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "401", description = "Autenticação inválida", content = @Content),
            @ApiResponse(responseCode = "403", description = "Permissão ADMIN necessária", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Relatório não encontrado", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Falha na exportação", content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<byte[]> export(@PathVariable String reportId,
                                  @CurrentUser CurrentUserData currentUser);
}
