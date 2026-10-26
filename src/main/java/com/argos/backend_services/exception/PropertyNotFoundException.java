package com.argos.backend_services.exception;

import com.argos.backend_services.exception.ArgosBadRequestException;
import org.springframework.http.HttpStatus;

public class PropertyNotFoundException extends ArgosBadRequestException {

    public PropertyNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "PROPERTY_NOT_FOUND", message);
    }
}
