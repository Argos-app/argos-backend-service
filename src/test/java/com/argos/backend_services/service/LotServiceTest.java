package com.argos.backend_services.service;

import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.dto.in.LotIn;
import com.argos.backend_services.dto.out.LotOut;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.exception.PropertyNotFoundException;
import com.argos.backend_services.mapper.LotMapper;
import com.argos.backend_services.repository.*;
import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.entity.Property;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class LotServiceTest {
    private LotService service(LotRepository lots, HerdRepository herds, DatabaseRoutineRepository routines,
                               LotMapper mapper, PropertyAccessService access) {
        return new LotService(lots, herds, routines, mapper, access, mock(ReportHerdService.class));
    }
    @Test void shouldRegisterLotUsingHerdName() throws LotNotFoundException, PropertyNotFoundException {
        var lots = mock(LotRepository.class); var routines = mock(DatabaseRoutineRepository.class);
        var mapper = mock(LotMapper.class); var herds = mock(HerdRepository.class); var access = mock(PropertyAccessService.class);
        var service = service(lots, herds, routines, mapper, access);
        var input = LotIn.builder().lotName("L-1").herdName("H-1").category("Engorda").headCount(2)
                .releaseSlaughterDate(LocalDate.of(2026, 7, 1)).build();
        var propertyId = UUID.randomUUID(); var property = mock(Property.class); when(property.getId()).thenReturn(propertyId);
        var now = LocalDateTime.of(2026, 1, 1, 10, 0);
        var herd = Herd.builder().propertyId(property).herdName("H-1").breed("Nelore").headCount(2)
                .active(true).createdAt(now).updatedAt(now).build();
        var lot = Lot.builder().herdId(herd).lotCode("L-1").category("Engorda").isExceptionLot(false)
                .headCount(2).fitForSlaughter(true).openDate(now.toLocalDate()).active(true)
                .createdAt(now).updatedAt(now).build();
        when(herds.findWithPropertyByHerdNameAndActiveTrue("H-1")).thenReturn(Optional.of(herd));
        when(mapper.toEntity(input, herd)).thenReturn(lot);
        service.registerLot(input, UUID.randomUUID());
        verify(lots).save(lot);
    }

    @Test void shouldThrowWhenHerdDoesNotExist() throws Exception {
        var mapper = mock(LotMapper.class); var herds = mock(HerdRepository.class);
        when(herds.findWithPropertyByHerdNameAndActiveTrue(any())).thenReturn(Optional.empty());
        var service = service(mock(LotRepository.class), herds, mock(DatabaseRoutineRepository.class), mapper,
                mock(PropertyAccessService.class));
        var input = LotIn.builder().lotName("L-1").herdName("missing").category("Engorda")
                .headCount(1).releaseSlaughterDate(LocalDate.now()).build();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.registerLot(input, UUID.randomUUID())).isInstanceOf(LotNotFoundException.class);
    }

    @Test void shouldSoftDeleteLot() throws LotNotFoundException {
        var lots = mock(LotRepository.class); var routines = mock(DatabaseRoutineRepository.class); var service = service(lots, mock(HerdRepository.class), routines, mock(LotMapper.class), mock(PropertyAccessService.class));
        var property = mock(Property.class); when(property.getId()).thenReturn(UUID.randomUUID());
        var now = LocalDateTime.of(2026, 1, 1, 10, 0);
        var herd = Herd.builder().propertyId(property).herdName("H-1").breed("Nelore").headCount(1)
                .active(true).createdAt(now).updatedAt(now).build();
        var lot = Lot.builder().herdId(herd).lotCode("L-1").category("Engorda").isExceptionLot(false)
                .headCount(1).fitForSlaughter(true).openDate(now.toLocalDate()).active(true)
                .createdAt(now).updatedAt(now).build(); var id = UUID.randomUUID(); when(lots.findWithHerdAndPropertyById(id)).thenReturn(Optional.of(lot));
        when(routines.lotIsReleaseForSlaughter(id, LocalDate.now())).thenReturn(true);
        service.deleteLot(id, UUID.randomUUID());
        assertThat(lot.isActive()).isFalse(); verify(lots).save(lot);
    }

    @Test void shouldThrowWhenLotDoesNotExist() {
        var lots = mock(LotRepository.class); var id = UUID.randomUUID(); when(lots.findWithHerdAndPropertyById(id)).thenReturn(Optional.empty());
        var service = service(lots, mock(HerdRepository.class), mock(DatabaseRoutineRepository.class), mock(LotMapper.class), mock(PropertyAccessService.class));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.deleteLot(id, UUID.randomUUID())).isInstanceOf(LotNotFoundException.class);
        verify(lots, never()).save(any());
    }

    @Test void shouldListOnlyActiveLots() {
        var lots = mock(LotRepository.class); var mapper = mock(LotMapper.class); var active = mock(Lot.class);
        var access = mock(PropertyAccessService.class); var userId = UUID.randomUUID(); var propertyIds = List.of(UUID.randomUUID());
        var out = LotOut.builder().lotName("L-1").build(); when(access.activePropertyIds(userId)).thenReturn(propertyIds); when(lots.findByActiveTrueAndHerdId_PropertyId_IdIn(propertyIds)).thenReturn(List.of(active)); when(mapper.toOut(active)).thenReturn(out);
        var result = service(lots, mock(HerdRepository.class), mock(DatabaseRoutineRepository.class), mapper, access).listLot(userId);
        assertThat(result).containsExactly(out); verify(mapper, times(1)).toOut(active);
    }

}
