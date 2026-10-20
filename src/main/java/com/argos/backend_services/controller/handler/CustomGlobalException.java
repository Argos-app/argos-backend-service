package com.argos.backend_services.controller.handler;

import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.exception.DeviceConflictException;
import com.argos.backend_services.exception.ArgosBadRequestException;
import com.argos.backend_services.exception.ArgosInternalServerException;
import com.argos.backend_services.exception.NotFoundException;
import com.argos.backend_services.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class CustomGlobalException {
    @ExceptionHandler(DeviceConflictException.class)
    ResponseEntity<ApiError> handleDeviceConflictException(DeviceConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(ex.getMessage(), HttpStatus.CONFLICT.toString(), "DEVICE_CONFLICT"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(new ApiError(
                ex.getReason(), ex.getStatusCode().toString(), HttpStatus.valueOf(ex.getStatusCode().value()).name()));
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ApiError> handleBadRequestException(BadRequestException ex) {
        var error = new ApiError(ex.getMessage(), HttpStatus.BAD_REQUEST.toString(), "BAD_REQUEST");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ArgosBadRequestException.class)
    ResponseEntity<ApiError> handleArgosBadRequestException(ArgosBadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ex.getMessage(), HttpStatus.BAD_REQUEST.toString(), "ARGOS_BAD_REQUEST"));
    }

    @ExceptionHandler(ArgosInternalServerException.class)
    ResponseEntity<ApiError> handleArgosInternalServerException(ArgosInternalServerException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.toString(), "ARGOS_INTERNAL_SERVER_ERROR"));
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> handleNotFoundException(NotFoundException ex) {
        var error = new ApiError(ex.getMessage(), HttpStatus.NOT_FOUND.toString(), "NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        var error = new ApiError(ex.getMessage(), HttpStatus.BAD_REQUEST.toString(), "METHOD_ARGUMENT_NOT_VALID");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ApiError> handleRuntimeException(RuntimeException ex) {
        var error = new ApiError(ex.getMessage(), HttpStatus.BAD_REQUEST.toString(), "RUNTIME_EXCEPTION");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex) {
        var error = new ApiError(ex.getMessage(), HttpStatus.BAD_REQUEST.toString(), "ILLEGAL_ARGUMENT_EXCEPTION");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(NumberFormatException.class)
    ResponseEntity<ApiError> handleNumberFormatException(NumberFormatException ex) {
        var error = new ApiError(ex.getMessage(), HttpStatus.BAD_REQUEST.toString(), "NUMBER_FORMAT");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ApiError> handleUnauthorizedException(UnauthorizedException ex) {
        var error = new ApiError(ex.getMessage(), HttpStatus.UNAUTHORIZED.toString(), "UNAUTHORIZED");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException ex) {
        var error = new ApiError(ex.getMessage(), HttpStatus.FORBIDDEN.toString(), "FORBIDDEN");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}
