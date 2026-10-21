package com.argos.backend_services.dto.out;

import com.argos.backend_services.entity.Property;

import java.math.BigDecimal;
import java.util.UUID;

public record PropertyOut(
        UUID id,
        String name,
        String cnpj,
        String address,
        String city,
        String state,
        BigDecimal areaHectares,
        String phone,
        Boolean active,
        UUID responsibleAdminId,
        String responsibleAdminName
) {
    public static PropertyOut from(Property property) {
        return new PropertyOut(
                property.getId(),
                property.getName(),
                trimToNull(property.getCnpj()),
                property.getAddress(),
                property.getCity(),
                trimToNull(property.getState()),
                property.getAreaHectares(),
                property.getPhone(),
                property.getActive(),
                property.getUser().getId(),
                property.getUser().getName());
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
