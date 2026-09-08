package com.argos.backend_services.service;

import com.argos.backend_services.dto.out.UserSignInResponseOut;
import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Permission;
import com.argos.backend_services.entity.UserCredential;
import com.argos.backend_services.repository.PropertyRepository;
import com.argos.backend_services.repository.UserCredentialRepository;
import com.argos.backend_services.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void shouldSignInUsingFirebaseEmailAndReturnApplicationJwt() throws Exception {
        String firebaseIdToken = "firebase-id-token";
        String email = "user@argos.com";
        UUID userId = UUID.randomUUID();

        UserRepository userRepository = mock(UserRepository.class);
        UserCredentialRepository userCredentialRepository = mock(UserCredentialRepository.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        FirebaseToken decodedToken = mock(FirebaseToken.class);

        Permission permission = Permission.builder().code("USER").name("User").build();
        AppUser user = AppUser.builder()
                .id(userId)
                .email(email)
                .name("Argos User")
                .permission(permission)
                .build();
        UserCredential credential = UserCredential.builder()
                .user(user)
                .userId(userId)
                .firstLogin(true)
                .build();
        user.setCredential(credential);

        when(firebaseAuth.verifyIdToken(firebaseIdToken)).thenReturn(decodedToken);
        when(decodedToken.getEmail()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(propertyRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of());

        JwtService jwtService = new JwtService(
                "test-secret-minimo-32-caracteres-para-hs256",
                86_400_000
        );
        AuthService authService = new AuthService(
                userRepository,
                userCredentialRepository,
                propertyRepository,
                jwtService
        );

        try (MockedStatic<FirebaseAuth> firebaseAuthStatic = mockStatic(FirebaseAuth.class)) {
            firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);

            UserSignInResponseOut response = authService.signIn(firebaseIdToken);

            assertEquals("Argos User", response.userName());
            assertEquals(email, response.email());
            assertNull(response.farmName());
            assertTrue(response.isFirstAccess());
            assertFalse(user.getCredential().getFirstLogin());
            assertEquals(userId, jwtService.extractUserId(response.bearerToken()));
            verify(userRepository).findByEmail(email);
            verify(userCredentialRepository).save(credential);
            verify(propertyRepository).findByUserIdAndActiveTrue(userId);
        }
    }
}
