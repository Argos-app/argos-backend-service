package com.argos.backend_services.dto.out;

import java.util.UUID;

public record CreateUserOut(UUID userId, String name, String email) {}
