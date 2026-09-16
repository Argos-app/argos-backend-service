package com.argos.backend_services.adapter.decoder;

import com.argos.backend_services.exception.IntegrationException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ErrorDecoderManagement implements ErrorDecoder {

    private List<ErrorDecoderIntegration> decoders;

    @Override
    public Exception decode(final String methodKey, final Response response) {
        return decoders.stream()
                .filter(decoder -> decoder.verify(response))
                .findFirst()
                .map(decoder -> decoder.execute(response))
                .orElseGet(() -> getDefaultException(response));
    }

    private static IntegrationException getDefaultException(final Response response) {
        final var ex = new Default().decode("", response);
        return new IntegrationException("feign", ex.getMessage());
    }
}
