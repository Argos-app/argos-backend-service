package com.argos.backend_services.controller;

import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.controller.annotation.FirebaseToken;
import com.argos.backend_services.dto.out.UserSignInResponseOut;
import com.argos.backend_services.security.CurrentUserData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/auth")
@Tag(name = "Auth", description = "Rotas de autenticação e autorização do usuário")
public interface AuthControllerApi {

    @PostMapping("/signin")
    @Operation(summary = "Autentica o usuário", description = "Valida o token do Firebase e retorna o token JWT usado nas rotas protegidas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário autenticado"),
            @ApiResponse(responseCode = "401", description = "Token Firebase ausente, inválido ou expirado")
    })
    ResponseEntity<UserSignInResponseOut> signIn(@Parameter(hidden = true) @FirebaseToken String idToken);

    @PutMapping("/first-access")
    @Operation(summary = "Conclui o primeiro acesso", description = "Atualiza o indicador de primeiro acesso do usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Primeiro acesso atualizado"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    })
    ResponseEntity<Void> updateFirstAccess(@Parameter(hidden = true) @CurrentUser CurrentUserData currentUser);
}
