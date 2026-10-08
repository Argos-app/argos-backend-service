package com.argos.backend_services.dto.out;

import java.time.LocalDate;
import java.util.UUID;

public record UserListItemOut(
        UUID userId,
        String photoUrl,
        String name,
        String cpf,
        String email,
        Boolean active,
        String farmName,
        String cnpj,
        LocalDate linkDate,
        UUID propertyId,
        UUID permissionId
) {
}
