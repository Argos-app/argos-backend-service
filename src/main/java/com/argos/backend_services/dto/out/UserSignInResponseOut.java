package com.argos.backend_services.dto.out;

public record UserSignInResponseOut(
        String bearerToken,
        String userName,
        String farmName,
        Boolean isFirstAccess
) {
}
