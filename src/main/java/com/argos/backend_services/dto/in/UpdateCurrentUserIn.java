package com.argos.backend_services.dto.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateCurrentUserIn(
        @Size(max = 120) String name,
        @Email @Size(max = 120) String email,
        @Size(max = 512) String photoUrl
) {
    public boolean isEmpty() {
        return name == null && email == null && photoUrl == null;
    }
}
