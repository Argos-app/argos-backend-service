package com.argos.backend_services.exception;

import org.springframework.http.HttpStatus;

public class MedicineNotFoundException extends ArgosBadRequestException{

    public MedicineNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "MEDICINE_NOT_FOUND", message);
    }
}
