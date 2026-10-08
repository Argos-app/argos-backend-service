package com.argos.backend_services.dto.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateUserIn(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Pattern(regexp = "\\d{11}") String cpf,
        @NotNull UUID permissionId,
        @NotNull UUID currentPropertyId,
        @NotNull UUID propertyId
) {}
