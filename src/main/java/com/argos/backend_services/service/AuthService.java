package com.argos.backend_services.service;

import com.argos.backend_services.exception.NotFoundException;
import com.argos.backend_services.exception.UnauthorizedException;
import com.argos.backend_services.exception.ArgosInternalServerException;
import com.argos.backend_services.dto.out.UserSignInResponseOut;
import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.repository.PropertyRepository;
import com.argos.backend_services.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.AuthErrorCode;
import org.springframework.http.HttpStatus;
import com.google.firebase.auth.FirebaseToken;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final JwtService jwtService;

    public UserSignInResponseOut signIn(String idToken) {
        FirebaseToken decodedToken;
        try {
            decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            if (e.getAuthErrorCode() == AuthErrorCode.CERTIFICATE_FETCH_FAILED) {
                log.error("firebase_verification_unavailable authCode={} errorCode={}",
                        e.getAuthErrorCode(), e.getErrorCode());
                throw new ArgosInternalServerException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "FIREBASE_UNAVAILABLE", "Não foi possível validar a autenticação no Firebase");
            }
            log.warn("firebase_token_rejected authCode={} errorCode={} reason={}",
                    e.getAuthErrorCode(), e.getErrorCode(), tokenRejectionReason(e));
            throw new UnauthorizedException("Token inválido ou expirado");
        }

        String email = decodedToken.getEmail();
        if (email == null || email.isBlank()) {
            log.warn("signin_rejected reason=firebase_email_missing");
            throw new UnauthorizedException("Token não possui e-mail");
        }

        Optional<AppUser> appUserOptional = userRepository.findByEmail(email);

        if (appUserOptional.isEmpty()) {
            log.warn("signin_rejected reason=user_not_registered_in_database");
            throw new NotFoundException("Usuário não encontrado");
        }

        AppUser appUser = appUserOptional.get();

        if (!Boolean.TRUE.equals(appUser.getActive())) {
            log.warn("signin_rejected reason=user_inactive");
            throw new UnauthorizedException("Usuário inativo");
        }
        if (!Boolean.TRUE.equals(appUser.getPermission().getActive())) {
            log.warn("signin_rejected reason=permission_inactive");
            throw new UnauthorizedException("Permissão do usuário inativa");
        }

        boolean isFirstAccess = appUser.getFirstLogin();
        if (isFirstAccess) {
            appUser.setFirstLogin(false);
            userRepository.save(appUser);
        }

        Property activeProperty = propertyRepository.findByUserIdAndActiveTrue(appUser.getId())
                .stream()
                .findFirst()
                .orElse(null);

        String farmName = activeProperty == null ? null : activeProperty.getName();
        String address = activeProperty == null ? null : activeProperty.getAddress();

        String bearerToken = jwtService.generate(appUser);

        return new UserSignInResponseOut(
                bearerToken,
                appUser.getName(),
                appUser.getEmail(),
                farmName,
                isFirstAccess,
                appUser.getId(),
                appUser.getPhotoUrl(),
                appUser.getCpf(),
                address);
    }

    public void updateFirstAccess(UUID uuid) {
        Optional<AppUser> userOptional = userRepository.findById(uuid);
        if (userOptional.isEmpty()) {
            throw new NotFoundException("Usuário não encontrado");
        }

        AppUser user = userOptional.get();
        user.setFirstLogin(false);

        userRepository.save(user);
    }

    private String tokenRejectionReason(FirebaseAuthException exception) {
        String message = exception.getMessage();
        if (message == null) return "unspecified";
        if (message.contains("incorrect \"aud\"")) return "audience_mismatch";
        if (message.contains("incorrect \"iss\"")) return "issuer_mismatch";
        if (message.contains("incorrect algorithm")) return "algorithm_mismatch";
        if (message.contains("has no \"kid\"")) return "missing_key_id";
        if (message.contains("signature")) return "invalid_signature";
        if (message.contains("has expired")) return "expired_token";
        return "token_verification_failed";
    }
}
