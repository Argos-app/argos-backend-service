package com.argos.backend_services.controller;

import com.argos.backend_services.controller.annotation.FirebaseToken;
import com.argos.backend_services.dto.out.UserSignInResponseOut;
import com.argos.backend_services.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("signin")
    public ResponseEntity<UserSignInResponseOut> signIn(@RequestHeader("Authorization") @FirebaseToken String idToken) {
        return ResponseEntity.ok(authService.signIn(idToken));
    }
}
