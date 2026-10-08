package com.argos.backend_services.controller;

import com.argos.backend_services.dto.in.PropertyIn;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.dto.out.PropertyOut;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/properties")
@Tag(name = "Properties", description = "Rotas de gestão de propriedades")
public interface PropertyControllerApi {

    @GetMapping
    @Operation(summary = "Lista propriedades ativas de forma paginada")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Propriedades encontradas", content = @Content(schema = @Schema(implementation = PageOut.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso permitido somente a administradores")
    })
    ResponseEntity<PageOut<PropertyOut>> listActive(
            @Parameter(description = "Índice da página, começando em zero") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Quantidade de registros por página") @RequestParam(defaultValue = "6") int size);

    @PostMapping
    @Operation(summary = "Cadastra uma propriedade e vincula o administrador responsável")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Propriedade criada", content = @Content(schema = @Schema(implementation = PropertyOut.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou CNPJ já cadastrado"),
            @ApiResponse(responseCode = "403", description = "Acesso permitido somente a administradores"),
            @ApiResponse(responseCode = "404", description = "Administrador responsável não encontrado")
    })
    ResponseEntity<PropertyOut> create(@Valid @RequestBody PropertyIn input);

    @PutMapping("/{propertyId}")
    @Operation(summary = "Atualiza os dados e o responsável da propriedade")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Propriedade atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso permitido somente a administradores"),
            @ApiResponse(responseCode = "404", description = "Propriedade ou administrador não encontrado")
    })
    ResponseEntity<Void> update(@Parameter(description = "Identificador da propriedade") @PathVariable UUID propertyId,
                                @Valid @RequestBody PropertyIn input);

    @DeleteMapping("/{propertyId}")
    @Operation(summary = "Desativa a propriedade e seus vínculos ativos com usuários")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Propriedade desativada"),
            @ApiResponse(responseCode = "403", description = "Acesso permitido somente a administradores"),
            @ApiResponse(responseCode = "404", description = "Propriedade não encontrada")
    })
    ResponseEntity<Void> deactivate(@Parameter(description = "Identificador da propriedade") @PathVariable UUID propertyId);
}
