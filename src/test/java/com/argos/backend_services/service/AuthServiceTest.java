package com.argos.backend_services.service;

import com.argos.backend_services.dto.out.UserSignInResponseOut;
import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Permission;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.exception.NotFoundException;
import com.argos.backend_services.exception.UnauthorizedException;
import com.argos.backend_services.exception.ArgosInternalServerException;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import com.argos.backend_services.repository.PropertyRepository;
import com.argos.backend_services.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(OutputCaptureExtension.class)
class AuthServiceTest {

    @Test
    void shouldRejectInvalidTokenAsUnauthorized(CapturedOutput output) throws Exception {
        FirebaseAuthException failure = mock(FirebaseAuthException.class);
        when(failure.getAuthErrorCode()).thenReturn(AuthErrorCode.INVALID_ID_TOKEN);
        when(failure.getMessage()).thenReturn("Firebase ID token has incorrect \"aud\" claim. sensitive-user-data");
        assertFirebaseFailure(failure, UnauthorizedException.class);
        assertTrue(output.getAll().contains("reason=audience_mismatch"));
        assertFalse(output.getAll().contains("sensitive-user-data"));
        assertFalse(output.getAll().contains("firebase-id-token"));
    }

    @Test
    void shouldNotReportCertificateFetchFailureAsInvalidCredentials() throws Exception {
        FirebaseAuthException failure = mock(FirebaseAuthException.class);
        when(failure.getAuthErrorCode()).thenReturn(AuthErrorCode.CERTIFICATE_FETCH_FAILED);
        assertFirebaseFailure(failure, ArgosInternalServerException.class);
    }

    private void assertFirebaseFailure(FirebaseAuthException failure,
                                      Class<? extends RuntimeException> expected) throws Exception {
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        when(firebaseAuth.verifyIdToken("firebase-id-token")).thenThrow(failure);
        AuthService service = new AuthService(mock(UserRepository.class),
                mock(PropertyRepository.class), mock(JwtService.class));
        try (MockedStatic<FirebaseAuth> firebaseAuthStatic = mockStatic(FirebaseAuth.class)) {
            firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            assertThrows(expected, () -> service.signIn("firebase-id-token"));
        }
    }

    @Test
    void shouldNotDeleteFirebaseUserWhenMissingFromThisDatabase() throws Exception {
        UserRepository users = mock(UserRepository.class);
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        FirebaseToken decodedToken = mock(FirebaseToken.class);
        when(firebaseAuth.verifyIdToken("firebase-id-token")).thenReturn(decodedToken);
        when(decodedToken.getEmail()).thenReturn("user@argos.com");
        when(users.findByEmail("user@argos.com")).thenReturn(Optional.empty());
        AuthService service = new AuthService(users, mock(PropertyRepository.class), mock(JwtService.class));

        try (MockedStatic<FirebaseAuth> firebaseAuthStatic = mockStatic(FirebaseAuth.class)) {
            firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            assertThrows(NotFoundException.class, () -> service.signIn("firebase-id-token"));
            verify(firebaseAuth, never()).deleteUser(anyString());
        }
    }

    @Test
    void shouldSignInUsingFirebaseEmailAndReturnApplicationJwt() throws Exception {
        String firebaseIdToken = "firebase-id-token";
        String email = "user@argos.com";
        UUID userId = UUID.randomUUID();

        UserRepository userRepository = mock(UserRepository.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        FirebaseToken decodedToken = mock(FirebaseToken.class);

        Permission permission = Permission.builder().code("USER").name("User").build();
        AppUser user = AppUser.builder()
                .id(userId)
                .email(email)
                .name("Argos User")
                .cpf("12345678901")
                .photoUrl("https://example.com/user.png")
                .permission(permission)
                .firstLogin(true)
                .build();

        Property property = Property.builder()
                .name("Fazenda Argos")
                .address("Estrada da Fazenda, 100")
                .build();

        when(firebaseAuth.verifyIdToken(firebaseIdToken)).thenReturn(decodedToken);
        when(decodedToken.getEmail()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(propertyRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(property));

        JwtService jwtService = new JwtService(
                "test-secret-minimo-32-caracteres-para-hs256",
                86_400_000
        );
        AuthService authService = new AuthService(
                userRepository,
                propertyRepository,
                jwtService
        );

        try (MockedStatic<FirebaseAuth> firebaseAuthStatic = mockStatic(FirebaseAuth.class)) {
            firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);

            UserSignInResponseOut response = authService.signIn(firebaseIdToken);

            assertEquals("Argos User", response.userName());
            assertEquals(email, response.email());
            assertEquals(userId, response.userId());
            assertEquals("https://example.com/user.png", response.photoUrl());
            assertEquals("12345678901", response.cpf());
            assertEquals("Estrada da Fazenda, 100", response.address());
            assertEquals("Fazenda Argos", response.farmName());
            assertTrue(response.isFirstAccess());
            assertFalse(user.getFirstLogin());
            assertEquals(userId, jwtService.extractUserId(response.bearerToken()));
            verify(userRepository).findByEmail(email);
            verify(userRepository).save(user);
            verify(propertyRepository).findByUserIdAndActiveTrue(userId);
        }
    }
}
