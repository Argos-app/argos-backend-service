package com.argos.backend_services.exception;

import org.springframework.http.HttpStatus;

public class InvalidDateWindowException extends ArgosBadRequestException {
    public InvalidDateWindowException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_DATE_WINDOW", message);
    }
}
