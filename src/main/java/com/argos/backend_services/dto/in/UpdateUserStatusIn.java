package com.argos.backend_services.dto.in;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusIn(@NotNull Boolean active) {}
