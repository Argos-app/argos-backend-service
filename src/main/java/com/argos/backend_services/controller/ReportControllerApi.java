package com.argos.backend_services.controller;

import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.controller.handler.ApiError;
import com.argos.backend_services.dto.out.HerdReportListItemOut;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.security.CurrentUserData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Relatórios", description = "Rotas de relatórios de rebanho")
public interface ReportControllerApi {

    @GetMapping(value = "/herds")
    @Operation(summary = "Lista os metadados dos relatórios de rebanho do administrador",
            description = "Considera as fazendas ativas administradas pelo usuário. Ordenação por reportCreatedAt e ID decrescentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de relatórios"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
            @ApiResponse(responseCode = "401", description = "Autenticação inválida"),
            @ApiResponse(responseCode = "403", description = "Permissão ADMIN ou acesso à fazenda necessário"),
            @ApiResponse(responseCode = "500", description = "Falha na listagem")
    })
    ResponseEntity<PageOut<HerdReportListItemOut>> list(
            @Parameter(description = "Fazenda administrada pelo usuário; opcional") @RequestParam(required = false) UUID propertyId,
            @Parameter(description = "Índice da página, começando em zero") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Registros por página, de 1 a 100") @RequestParam(defaultValue = "6") int size,
            @CurrentUser CurrentUserData currentUser);

    @GetMapping("/herds/{reportId}/export")
    @Operation(summary = "Exporta um relatório de rebanho em XLSX",
            description = "Recebe o ID do documento MongoDB. Exclusivo para administradores da fazenda ativa do relatório.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo XLSX", content = @Content(
                    mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "401", description = "Autenticação inválida"),
            @ApiResponse(responseCode = "403", description = "Permissão ADMIN necessária"),
            @ApiResponse(responseCode = "404", description = "Relatório não encontrado"),
            @ApiResponse(responseCode = "500", description = "Falha na exportação")
    })
    ResponseEntity<byte[]> export(@PathVariable String reportId,
                                  @CurrentUser CurrentUserData currentUser);
}
