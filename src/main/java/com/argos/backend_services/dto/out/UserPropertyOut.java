package com.argos.backend_services.dto.out;

import java.time.LocalDate;
import java.util.UUID;

public record UserPropertyOut(UUID id, String name, String cnpj, LocalDate linkDate) { }
