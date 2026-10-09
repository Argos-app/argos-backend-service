package com.argos.backend_services.dto.in;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record PropertyIn(
        @NotBlank @Size(max = 120) String name,
        @Pattern(regexp = "^$|^\\d{14}$|^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$")
        String cnpj,
        @NotBlank @Size(max = 255) String address,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO")
        String state,
        @Positive @Digits(integer = 8, fraction = 2) BigDecimal areaHectares,
        @Size(max = 20) String phone,
        @NotNull UUID responsibleAdminId
) {}
