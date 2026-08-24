package com.argos.backend_services.controller.handler;

import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class CustomGlobalExceptionTest {

    private final CustomGlobalException handler = new CustomGlobalException();

    @Test
    void shouldReturnBadRequestException() {
        String message = "Requisição inválida";
        BadRequestException exception = new BadRequestException(message);

        ResponseEntity<ApiError> response =
                handler.handleBadRequestException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(message, response.getBody().message());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), response.getBody().code());
        assertEquals("HANDLE_BAD_REQUEST", response.getBody().informationCode());
    }

    @Test
    void shouldReturnRuntimeException() {
        String message = "Erro interno";
        RuntimeException exception = new RuntimeException(message);

        ResponseEntity<ApiError> response =
                handler.handleRuntimeException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(message, response.getBody().message());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), response.getBody().code());
        assertEquals(
                "RUNTIME_EXCEPTION",
                response.getBody().informationCode()
        );
    }

    @Test
    void shouldReturnIllegalArgumentException() {
        String message = "Argumento inválido";
        IllegalArgumentException exception =
                new IllegalArgumentException(message);

        ResponseEntity<ApiError> response =
                handler.handleIllegalArgumentException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(message, response.getBody().message());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), response.getBody().code());
        assertEquals(
                "ILLEGAL_ARGUMENT_EXCEPTION",
                response.getBody().informationCode()
        );
    }

    @Test
    void shouldReturnNumberFormatException() {
        String message = "For input string: abc";
        NumberFormatException exception =
                new NumberFormatException(message);

        ResponseEntity<ApiError> response =
                handler.handleNumberFormatException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(message, response.getBody().message());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), response.getBody().code());
        assertEquals(
                "NUMBER_FORMAT",
                response.getBody().informationCode()
        );
    }
}