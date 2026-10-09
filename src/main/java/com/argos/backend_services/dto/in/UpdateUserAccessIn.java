package com.argos.backend_services.dto.in;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateUserAccessIn(@NotNull UUID permissionId) {}
