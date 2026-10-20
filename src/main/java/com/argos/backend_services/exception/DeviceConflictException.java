package com.argos.backend_services.exception;

import org.springframework.http.HttpStatus;

public class DeviceConflictException extends ArgosBadRequestException {
    public DeviceConflictException(String message) {
        super(HttpStatus.CONFLICT, "DEVICE_CONFLICT", message);
    }
}
