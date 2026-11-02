package com.argos.backend_services.controller;

import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.dto.in.LotIn;
import com.argos.backend_services.dto.out.LotOut;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.exception.PropertyNotFoundException;
import com.argos.backend_services.security.CurrentUserData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RequestMapping("/lot")
@Tag(name = "Lotes", description = "Gerenciamento de lotes")

public interface LotControllerApi {

    @GetMapping("/list-all")
    @Operation(summary = "Lista lotes ativos", description = "Inclui dados de animais em lotes de exceção.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lotes encontrados"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos", content = @Content(schema = @Schema(example = "{\"message\":\"Parâmetro inválido\"}"))),
            @ApiResponse(responseCode = "422", description = "Não foi possível processar a solicitação", content = @Content(examples = @ExampleObject(value = "{\"message\":\"Falha ao consultar lotes\"}")))
    })
    ResponseEntity<List<LotOut>> listLot(@CurrentUser CurrentUserData currentUser);

    @PostMapping
    @Operation(summary = "Cadastra lote", description = "Usa herdId para selecionar um rebanho ativo. Sem herdId, cria um rebanho com herdName e breed na propriedade do contexto autenticado ou na única propriedade ativa acessível. A contagem do rebanho é calculada pelos lotes ativos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lote cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou propriedade ambígua"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Usuário sem acesso à propriedade"),
            @ApiResponse(responseCode = "404", description = "Rebanho ou propriedade não encontrado"),
            @ApiResponse(responseCode = "409", description = "Nome de rebanho já cadastrado na propriedade")
    })
    ResponseEntity<Void> registerLot(@Valid @RequestBody LotIn lotIn,
                                     @CurrentUser CurrentUserData currentUser) throws LotNotFoundException, PropertyNotFoundException;

    @DeleteMapping("/{lotId}")
    @Operation(summary = "Exclui lote", description = "Executa exclusão lógica.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Lote excluído"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido", content = @Content(examples = @ExampleObject(value = "{\"message\":\"lotId inválido\"}"))),
            @ApiResponse(responseCode = "422", description = "Lote não encontrado ou não pode ser excluído", content = @Content(examples = @ExampleObject(value = "{\"message\":\"Lot not found\"}")))
    })
    ResponseEntity<Void> deleteLot(@PathVariable UUID lotId,
                                   @CurrentUser CurrentUserData currentUser) throws LotNotFoundException;
}
