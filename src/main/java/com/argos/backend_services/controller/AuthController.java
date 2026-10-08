package com.argos.backend_services.controller;

import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.controller.annotation.FirebaseToken;
import com.argos.backend_services.dto.out.UserSignInResponseOut;
import com.argos.backend_services.security.CurrentUserData;
import com.argos.backend_services.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerApi {
    private final AuthService authService;

    @Override
    public ResponseEntity<UserSignInResponseOut> signIn(@FirebaseToken String idToken) {
        return ResponseEntity.ok(authService.signIn(idToken));
    }

    @Override
    public ResponseEntity<Void> updateFirstAccess(@CurrentUser CurrentUserData currentUser) {
        UUID uuid = currentUser.id();
        authService.updateFirstAccess(uuid);
        return ResponseEntity.noContent().build();
    }
}
