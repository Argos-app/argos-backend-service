package com.argos.backend_services.dto.out;

import java.util.List;
import java.util.UUID;

public record UserProfileOut(
        UUID userId,
        String name,
        String email,
        String cpf,
        String photoUrl,
        String permissionName,
        String permissionCode,
        boolean active,
        List<UserProfilePropertyOut> properties
) { }
