package com.argos.backend_services.exception;

import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Builder
@NoArgsConstructor
public class ArgosInternalServerException extends RuntimeException {

    HttpStatus code;
    String informationCode;
    String message;

    public ArgosInternalServerException(HttpStatus code, String informationCode, String message) {
        super(message);
        this.code = code;
        this.informationCode = informationCode;
        this.message = message;
    }

}
