package com.argos.backend_services.exception;

import org.springframework.http.HttpStatus;

public class GeneralizedException extends ArgosInternalServerException{

    public GeneralizedException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", message);
    }
}
