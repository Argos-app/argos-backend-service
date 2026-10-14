package com.argos.backend_services.controller;

import com.argos.backend_services.controller.handler.ApiError;
import com.argos.backend_services.dto.in.MedicineBatchIn;
import com.argos.backend_services.dto.out.MedicineCreatedOut;
import com.argos.backend_services.dto.out.MedicineListItemOut;
import com.argos.backend_services.dto.out.PageOut;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@RequestMapping("/medicines")
@Tag(name = "Medicamentos", description = "Entrada e consulta do estoque de medicamentos")
public interface MedicineControllerApi {

    @PostMapping
    @Operation(summary = "Registra a entrada de um lote de medicamento",
            description = "Localiza o medicamento pelo registro MAPA, cria o fornecedor por CNPJ quando necessário e registra a entrada inicial no estoque.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lote de medicamento registrado",
                    content = @Content(schema = @Schema(implementation = MedicineCreatedOut.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos, lote duplicado ou validade inválida",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Medicamento MAPA ou usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<MedicineCreatedOut> create(
            @Valid @RequestBody MedicineBatchIn input,
            @Parameter(description = "Identificador do usuário que registrou a entrada", required = true)
            @RequestHeader UUID userId);

    @GetMapping
    @Operation(summary = "Lista os lotes de medicamentos disponíveis",
            description = "Retorna nome, fornecedor, fabricação, validade, dose, saldo, carência, lote, princípio ativo e categoria terapêutica. O filtro de categoria é opcional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de medicamentos disponíveis",
                    content = @Content(schema = @Schema(implementation = PageOut.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<PageOut<MedicineListItemOut>> list(
            @Parameter(description = "Identificador opcional da categoria terapêutica", schema = @Schema(format = "uuid"))
            @RequestParam(required = false) UUID therapeuticCategoryId,
            @Parameter(description = "Página baseada em zero", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Quantidade de itens por página, entre 1 e 100", example = "20")
            @RequestParam(defaultValue = "20") int size);
}
