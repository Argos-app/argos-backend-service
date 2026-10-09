package com.argos.backend_services.dto.out;

import com.argos.backend_services.entity.AppUser;

import java.util.UUID;

public record AdminUserOptionOut(UUID id, String name, String email) {
    public static AdminUserOptionOut from(AppUser user) {
        return new AdminUserOptionOut(user.getId(), user.getName(), user.getEmail());
    }
}
