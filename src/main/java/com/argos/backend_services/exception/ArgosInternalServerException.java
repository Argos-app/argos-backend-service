package com.argos.backend_services.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArgosInternalServerException extends RuntimeException {

    HttpStatus code;
    String informationCode;
    String message;
}
