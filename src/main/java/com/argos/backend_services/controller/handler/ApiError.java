package com.argos.backend_services.controller.handler;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiError {

    private String message;
    private String code;
    private String informationCode;

    public ApiError (String message, String code, String informationCode) {
        this.message = message;
        this.code = code;
        this.informationCode = informationCode;
    }
}