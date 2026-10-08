package com.argos.backend_services.service;

import com.argos.backend_services.exception.NotFoundException;
import com.argos.backend_services.exception.UnauthorizedException;
import com.argos.backend_services.dto.out.UserSignInResponseOut;
import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.repository.PropertyRepository;
import com.argos.backend_services.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final JwtService jwtService;

    public UserSignInResponseOut signIn(String idToken) {
        FirebaseToken decodedToken;
        try {
            decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            throw new UnauthorizedException("Token inválido ou expirado");
        }

        String email = decodedToken.getEmail();
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Token não possui e-mail");
        }

        Optional<AppUser> appUserOptional = userRepository.findByEmail(email);

        if (appUserOptional.isEmpty()) {
            try {
                FirebaseAuth.getInstance().deleteUser(decodedToken.getUid());
            } catch (FirebaseAuthException e) {
                throw new UnauthorizedException("Erro ao remover usuário do Firebase Authentication");
            }
            throw new NotFoundException("Usuário não encontrado");
        }

        AppUser appUser = appUserOptional.get();

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
}
