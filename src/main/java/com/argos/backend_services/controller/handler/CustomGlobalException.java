package com.argos.backend_services.controller.handler;

import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomGlobalException {

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ApiError> handleBadRequestException(BadRequestException ex) {
        var error = new ApiError(ex.getMessage(), HttpStatus.BAD_REQUEST.toString(), "HANDLE_BAD_REQUEST");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        var error = new ApiError(ex.getMessage(), HttpStatus.BAD_REQUEST.toString(), "METHOD_ARGUMENT_NOT_VALID");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ApiError> handleRuntimeException(RuntimeException ex) {
        var error = new ApiError(ex.getMessage(), HttpStatus.BAD_REQUEST.toString(), "METHOD_ARGUMENT_NOT_VALID");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex) {
        var error = new ApiError(ex.getMessage(), HttpStatus.BAD_REQUEST.toString(), "METHOD_ARGUMENT_NOT_VALID");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(NumberFormatException.class)
    ResponseEntity<ApiError> handleNumberFormatException(NumberFormatException ex) {
        var error = new ApiError(ex.getMessage(), HttpStatus.BAD_REQUEST.toString(), "METHOD_ARGUMENT_NOT_VALID");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}