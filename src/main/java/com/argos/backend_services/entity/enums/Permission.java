package com.argos.backend_services.entity.enums;

public enum Permission {
    ADMIN("ADMIN"),
    GERENTE("GERENTE"),
    PECUARISTA("PECUARISTA"),
    APLICADOR("APLICADOR"),
    SISTEMA("SISTEMA");

    private final String permission;

    Permission(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }
}