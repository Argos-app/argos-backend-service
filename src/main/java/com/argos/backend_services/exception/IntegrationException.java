package com.argos.backend_services.exception;

import org.springframework.http.HttpStatus;

public class IntegrationException extends ArgosInternalServerException{

    public IntegrationException(final String service, final String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "INTEGRATION_ERROR", "integration error on " + service + message);
    }
}
