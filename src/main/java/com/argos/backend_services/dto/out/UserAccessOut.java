package com.argos.backend_services.dto.out;

import java.util.UUID;

public record UserAccessOut(
        UUID userId,
        String photoUrl,
        String name,
        String email,
        Boolean active,
        UUID permissionId,
        String permissionName
) {}
