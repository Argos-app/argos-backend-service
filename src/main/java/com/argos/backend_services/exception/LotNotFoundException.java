package com.argos.backend_services.exception;

import org.springframework.http.HttpStatus;

public class LotNotFoundException extends ArgosBadRequestException{

    public LotNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "LOT_NOT_FOUND", message);
    }
}
