package com.argos.backend_services.security;

import java.util.UUID;

public record CurrentUserData(
        UUID id,
        String email,
        String name,
        String permission,
        UUID propertyId
) {
}
