package com.argos.backend_services.dto.out;

import java.util.UUID;

public record UserSignInResponseOut(
        String bearerToken,
        String userName,
        String email,
        String farmName,
        Boolean isFirstAccess,
        UUID userId,
        String photoUrl,
        String cpf,
        String address
) {
}
