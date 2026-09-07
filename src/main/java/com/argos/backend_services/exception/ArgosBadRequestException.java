package com.argos.backend_services.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArgosBadRequestException extends BadRequestException {

    HttpStatus code;
    String informationCode;
    String message;
}
