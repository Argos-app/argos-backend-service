package com.argos.backend_services.dto.in;

import com.argos.backend_services.entity.enums.DevicePlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DeviceSyncIn(
        UUID deviceId,
        @NotBlank @Size(max = 512)
        @Schema(description = "Token FCM", maxLength = 512)
        String pushToken,
        @NotNull DevicePlatform platform,
        @Size(max = 60) String model) {
}
