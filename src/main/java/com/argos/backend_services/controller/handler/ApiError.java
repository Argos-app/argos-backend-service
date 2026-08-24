package com.argos.backend_services.controller.handler;

public record ApiError (

    String message,
    String code,
    String informationCode
) {}
