package com.argos.backend_services.entity.enums;

public enum HerdCategory {
    FATTENING("ENGORDA"),
    BREEDING("CRIA"),
    REARING("RECRIA");

    private final String description;

    HerdCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
