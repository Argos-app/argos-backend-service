package com.argos.backend_services.dto.out;

import java.util.List;
import java.util.UUID;

public record UserListItemOut(
        UUID userId,
        String photoUrl,
        String name,
        String cpf,
        String email,
        Boolean active,
        UUID permissionId,
        List<UserPropertyOut> properties
) {
    public UserListItemOut(UUID userId, String photoUrl, String name, String cpf, String email,
                           Boolean active, UUID permissionId) {
        this(userId, photoUrl, name, cpf, email, active, permissionId, List.of());
    }
}
