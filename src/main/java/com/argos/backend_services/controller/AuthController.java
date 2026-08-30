package com.argos.backend_services.controller;

import com.argos.backend_services.controller.annotation.FirebaseToken;
import com.argos.backend_services.dto.out.UserSignInResponseOut;
import com.argos.backend_services.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(
name = "Auth", description = "Rotas de autenticação e autorização do usuário"
)

public class AuthController {
    private final AuthService authService;

    @PostMapping("signin")
    @Operation(summary = "Autentic                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      ação do usuário",
            description = "Autentica o usuário com o token do Firebase e retorna um token JWT para acesso às rotas protegidas")
    public ResponseEntity<UserSignInResponseOut> signIn(@FirebaseToken String idToken) {
        return ResponseEntity.ok(authService.signIn(idToken));
    }
}
