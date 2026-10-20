package com.argos.backend_services.exception;

public class DeviceConflictException extends RuntimeException {
    public DeviceConflictException() {
        this(null);
    }

    public DeviceConflictException(Throwable cause) {
        super("O token já está associado a outro dispositivo. Desative o vínculo anterior antes de sincronizar.", cause);
    }
}
