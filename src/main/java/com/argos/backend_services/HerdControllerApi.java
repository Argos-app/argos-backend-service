package com.argos.backend_services;

import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.dto.HerdOut;
import com.argos.backend_services.security.CurrentUserData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Tag(name = "Rebanho", description = "Rotas de gestão de rebanho")
public interface HerdControllerApi {
    @GetMapping("/list-all")
    @Operation(summary = "Lista todos os animais do rebanho da propriedade do usuário autenticado",
            description = "Retorna uma lista de todos os animais do rebanho da propriedade vinculada ao usuário autenticado. " +
                    "O usuário deve ter permissão para acessar a propriedade.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de animais do rebanho retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Usuário sem permissão para acessar a propriedade"),
            @ApiResponse(responseCode = "404", description = "Propriedade vinculada ao usuário não encontrada")
    })
    ResponseEntity<List<HerdOut>> listHerd(@CurrentUser CurrentUserData currentUserData);
}
