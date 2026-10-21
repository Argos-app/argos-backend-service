package com.argos.backend_services.dto.in;

import java.util.UUID;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserIn(
        @NotBlank @Size(max = 120)
        String name,
        @NotBlank @Email @Size(max = 120)
        String email,
        @NotBlank @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 dígitos")
        String cpf,
        @NotBlank @Size(min = 8, max = 128)
        String password,
        @NotNull
        UUID permissionId,
        @NotNull
        UUID propertyId
) {
}
