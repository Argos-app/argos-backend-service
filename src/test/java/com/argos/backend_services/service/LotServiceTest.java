package com.argos.backend_services.service;

import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.dto.in.LotIn;
import com.argos.backend_services.dto.out.LotOut;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.mapper.LotMapper;
import com.argos.backend_services.repository.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class LotServiceTest {
    private LotService service(LotRepository lots, DatabaseRoutineRepository routines, LotMapper mapper) {
        return new LotService(lots, routines, mapper);
    }
    @Test void shouldRegisterLotUsingHerdName() throws LotNotFoundException {
        var lots = mock(LotRepository.class); var routines = mock(DatabaseRoutineRepository.class);
        var mapper = mock(LotMapper.class); var service = service(lots, routines, mapper);
        var input = LotIn.builder().lotName("L-1").herdName("H-1").category("Engorda").headCount(2).build();
        var lot = Lot.builder().build();
        when(mapper.toEntity(input)).thenReturn(lot);
        service.registerLot(input);
        verify(lots).save(lot);
    }

    @Test void shouldThrowWhenHerdDoesNotExist() throws Exception {
        var mapper = mock(LotMapper.class);
        when(mapper.toEntity(any())).thenThrow(new LotNotFoundException("Herd not found"));
        var service = service(mock(LotRepository.class), mock(DatabaseRoutineRepository.class), mapper);
        var input = LotIn.builder().herdName("missing").build();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.registerLot(input)).isInstanceOf(LotNotFoundException.class);
    }

    @Test void shouldSoftDeleteLot() throws LotNotFoundException {
        var lots = mock(LotRepository.class); var routines = mock(DatabaseRoutineRepository.class); var service = service(lots, routines, mock(LotMapper.class));
        var lot = Lot.builder().active(true).build(); var id = UUID.randomUUID(); when(lots.findById(id)).thenReturn(Optional.of(lot));
        when(routines.lotIsReleaseForSlaughter(id, LocalDate.now())).thenReturn(true);
        service.deleteLot(id, UUID.randomUUID());
        assertThat(lot.isActive()).isFalse(); verify(lots).save(lot);
    }

    @Test void shouldThrowWhenLotDoesNotExist() {
        var lots = mock(LotRepository.class); var id = UUID.randomUUID(); when(lots.findById(id)).thenReturn(Optional.empty());
        var service = service(lots, mock(DatabaseRoutineRepository.class), mock(LotMapper.class));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.deleteLot(id, UUID.randomUUID())).isInstanceOf(LotNotFoundException.class);
        verify(lots, never()).save(any());
    }

    @Test void shouldListOnlyActiveLots() {
        var lots = mock(LotRepository.class); var mapper = mock(LotMapper.class); var active = Lot.builder().active(true).build(); var inactive = Lot.builder().active(false).build();
        var out = LotOut.builder().lotName("L-1").build(); when(lots.findAll()).thenReturn(List.of(active, inactive)); when(mapper.toOut(active)).thenReturn(out);
        var result = service(lots, mock(DatabaseRoutineRepository.class), mapper).listLot(UUID.randomUUID());
        assertThat(result).containsExactly(out); verify(mapper, times(1)).toOut(active);
    }

}
