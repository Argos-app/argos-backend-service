package com.argos.backend_services.controller;

import com.argos.backend_services.controller.impl.LotController;
import com.argos.backend_services.dto.in.LotIn;
import com.argos.backend_services.dto.out.LotOut;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.service.LotService;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LotControllerTest {
    @Test void shouldReturnLots() {
        var service = mock(LotService.class); var controller = new LotController(service); var user = UUID.randomUUID(); var expected = List.of(LotOut.builder().lotName("L-1").build());
        when(service.listLot(user)).thenReturn(expected);
        var response = controller.listLot(user);
        assertThat(response.getStatusCode().value()).isEqualTo(200); assertThat(response.getBody()).isEqualTo(expected); verify(service).listLot(user);
    }
    @Test void shouldRegisterLotAndReturnOk() throws LotNotFoundException {
        var service = mock(LotService.class); var controller = new LotController(service); var input = LotIn.builder().lotName("L-1").herdName("H-1").build();
        var response = controller.registerLot(input);
        assertThat(response.getStatusCode().value()).isEqualTo(200); verify(service).registerLot(input);
    }
    @Test void shouldDeleteLotAndReturnNoContent() throws LotNotFoundException {
        var service = mock(LotService.class); var controller = new LotController(service); var id = UUID.randomUUID(); var user = UUID.randomUUID();
        var response = controller.deleteLot(id, user);
        assertThat(response.getStatusCode().value()).isEqualTo(204); verify(service).deleteLot(id, user);
    }
}
