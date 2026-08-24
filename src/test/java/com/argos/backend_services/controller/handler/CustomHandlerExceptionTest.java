package com.argos.backend_services.controller.handler;

import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;

class CustomGlobalExceptionTest {

    private final CustomGlobalException handler = new CustomGlobalException();

    @Test
    void deveTratarBadRequestException() {
        String mensagem = "Requisição inválida";
        BadRequestException exception = new BadRequestException(mensagem);

        ResponseEntity<ApiError> response =
                handler.handleBadRequestException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mensagem, response.getBody().message());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), response.getBody().code());
        assertEquals("HANDLE_BAD_REQUEST", response.getBody().code());
    }

    @Test
    void deveTratarMethodArgumentNotValidException() {
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, null);

        ResponseEntity<ApiError> response =
                handler.handleMethodArgumentNotValidException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(
                "METHOD_ARGUMENT_NOT_VALID",
                response.getBody().code()
        );
    }

    @Test
    void deveTratarRuntimeException() {
        String mensagem = "Erro interno";
        RuntimeException exception = new RuntimeException(mensagem);

        ResponseEntity<ApiError> response =
                handler.handleRuntimeException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mensagem, response.getBody().message());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), response.getBody().code());
        assertEquals(
                "METHOD_ARGUMENT_NOT_VALID",
                response.getBody().code()
        );
    }

    @Test
    void deveTratarIllegalArgumentException() {
        String mensagem = "Argumento inválido";
        IllegalArgumentException exception =
                new IllegalArgumentException(mensagem);

        ResponseEntity<ApiError> response =
                handler.handleIllegalArgumentException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mensagem, response.getBody().message());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), response.getBody().code());
        assertEquals(
                "METHOD_ARGUMENT_NOT_VALID",
                response.getBody().code()
        );
    }

    @Test
    void deveTratarNumberFormatException() {
        String mensagem = "For input string: abc";
        NumberFormatException exception =
                new NumberFormatException(mensagem);

        ResponseEntity<ApiError> response =
                handler.handleNumberFormatException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mensagem, response.getBody().message());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), response.getBody().code());
        assertEquals(
                "METHOD_ARGUMENT_NOT_VALID",
                response.getBody().code()
        );
    }
}