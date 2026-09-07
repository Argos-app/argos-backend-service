package com.argos.backend_services.adapter.decoder;

import com.argos.backend_services.exception.IntegrationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;

import java.io.IOException;

public interface ErrorDecoderIntegration {

    boolean verify(Response response);

    RuntimeException execute(Response response);

    default Integer getHttpStatusCode(final Response response) {
        return response.status();
    }

    default <T> T getErrorMessage(final Response response, final Class<T> resultClass, final ObjectMapper objectMapper) {
        try (var inputStream = response.body().asInputStream()) {
            return objectMapper.readValue(inputStream, resultClass);
        } catch (IOException e) {
            throw new IntegrationException(
                    "decode-error",
                    "Error to desirialized resposta: " + e.getMessage()
            );
        }
    }
}
