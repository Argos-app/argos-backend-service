package com.argos.backend_services.dto.out;

public interface CurrentUserAccessOut {
    Boolean getUserActive();
    Boolean getPermissionActive();
    String getPermissionCode();
}
