package com.argos.backend_services.service;

import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.entity.AnimalException;
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
    private LotService service(LotRepository lots, HerdRepository herds, AnimalExceptionRepository animals, ApplicationAnimalRepository applications, LotMapper mapper) {
        return new LotService(lots, herds, animals, applications, mapper);
    }
    @Test void shouldRegisterLotUsingHerdName() throws LotNotFoundException {
        var lots = mock(LotRepository.class); var herds = mock(HerdRepository.class);
        var animals = mock(AnimalExceptionRepository.class); var applications = mock(ApplicationAnimalRepository.class);
        var mapper = mock(LotMapper.class); var service = service(lots, herds, animals, applications, mapper);
        var input = LotIn.builder().lotName("L-1").herdName("H-1").category("Engorda").headCount(2).build();
        var herd = Herd.builder().herdName("H-1").build(); var lot = Lot.builder().build();
        when(herds.findByHerdNameAndActiveTrue("H-1")).thenReturn(Optional.of(herd)); when(mapper.toEntity(input)).thenReturn(lot);
        service.registerLot(input);
        assertThat(lot.getHerdId()).isSameAs(herd); verify(lots).save(lot);
    }

    @Test void shouldThrowWhenHerdDoesNotExist() {
        var herds = mock(HerdRepository.class); when(herds.findByHerdNameAndActiveTrue("missing")).thenReturn(Optional.empty());
        var service = service(mock(LotRepository.class), herds, mock(AnimalExceptionRepository.class), mock(ApplicationAnimalRepository.class), mock(LotMapper.class));
        var input = LotIn.builder().herdName("missing").build();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.registerLot(input)).isInstanceOf(LotNotFoundException.class);
    }

    @Test void shouldSoftDeleteLot() throws LotNotFoundException {
        var lots = mock(LotRepository.class); var service = new LotService(lots, mock(HerdRepository.class), mock(AnimalExceptionRepository.class), mock(ApplicationAnimalRepository.class), mock(LotMapper.class));
        var lot = Lot.builder().active(true).build(); var id = UUID.randomUUID(); when(lots.findById(id)).thenReturn(Optional.of(lot));
        service.deleteLot(id, UUID.randomUUID());
        assertThat(lot.isActive()).isFalse(); verify(lots).save(lot);
    }

    @Test void shouldThrowWhenLotDoesNotExist() {
        var lots = mock(LotRepository.class); var id = UUID.randomUUID(); when(lots.findById(id)).thenReturn(Optional.empty());
        var service = service(lots, mock(HerdRepository.class), mock(AnimalExceptionRepository.class), mock(ApplicationAnimalRepository.class), mock(LotMapper.class));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.deleteLot(id, UUID.randomUUID())).isInstanceOf(LotNotFoundException.class);
        verify(lots, never()).save(any());
    }

    @Test void shouldListOnlyActiveLots() {
        var lots = mock(LotRepository.class); var mapper = mock(LotMapper.class); var active = Lot.builder().active(true).build(); var inactive = Lot.builder().active(false).build();
        var out = LotOut.builder().lotName("L-1").build(); when(lots.findAll()).thenReturn(List.of(active, inactive)); when(mapper.toOut(active)).thenReturn(out);
        var result = service(lots, mock(HerdRepository.class), mock(AnimalExceptionRepository.class), mock(ApplicationAnimalRepository.class), mapper).listLot(UUID.randomUUID());
        assertThat(result).containsExactly(out); verify(mapper, times(1)).toOut(active);
    }

    @Test void shouldMapExceptionAnimalWithoutApplication() {
        var lots = mock(LotRepository.class); var animals = mock(AnimalExceptionRepository.class); var mapper = mock(LotMapper.class);
        var lot = Lot.builder().active(true).isExceptionLot(true).build(); var animal = AnimalException.builder().lotId(lot).earTag("E-1").slaughterReleaseDate(LocalDate.of(2026, 7, 1)).build();
        var out = LotOut.builder().build(); when(lots.findAll()).thenReturn(List.of(lot)); when(mapper.toOut(lot)).thenReturn(out); when(animals.findByLotId(lot)).thenReturn(List.of(animal));
        service(lots, mock(HerdRepository.class), animals, mock(ApplicationAnimalRepository.class), mapper).listLot(UUID.randomUUID());
        assertThat(out.getAnimalExceptionOutList()).singleElement().satisfies(item -> { assertThat(item.getEarCode()).isEqualTo("E-1"); assertThat(item.getMedicine()).isNull(); assertThat(item.getReleaseSlaughterDate()).isEqualTo("2026-07-01"); });
    }
}
