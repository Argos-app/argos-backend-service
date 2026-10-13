package com.argos.backend_services.controller;

import com.argos.backend_services.controller.impl.AnimalExceptionController;
import com.argos.backend_services.dto.in.AnimalExceptionIn;
import com.argos.backend_services.service.AnimalExceptionService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AnimalExceptionControllerTest {

    @Test
    void shouldCreateAnimalExceptionUsingUserHeader() throws Exception {
        var service = mock(AnimalExceptionService.class);
        var controller = new AnimalExceptionController(service);
        var userId = UUID.randomUUID();
        var input = new AnimalExceptionIn(UUID.randomUUID(), "BR-001", "Doença respiratória", new BigDecimal("420"));

        var response = controller.create(input, userId);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        verify(service).register(input, userId);
    }
}
