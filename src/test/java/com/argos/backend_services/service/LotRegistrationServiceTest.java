package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.LotIn;
import com.argos.backend_services.entity.enums.HerdCategory;
import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.exception.NotFoundException;
import com.argos.backend_services.mapper.LotMapper;
import com.argos.backend_services.repository.*;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LotRegistrationServiceTest {
    private final LotRepository lots = mock(LotRepository.class);
    private final HerdRepository herds = mock(HerdRepository.class);
    private final PropertyRepository properties = mock(PropertyRepository.class);
    private final PropertyAccessService access = mock(PropertyAccessService.class);
    private final ReportHerdService reports = mock(ReportHerdService.class);
    private final LotMapper mapper = new LotMapper(mock(AnimalExceptionRepository.class), mock(ApplicationAnimalRepository.class));
    private final LotService service = new LotService(lots, herds, mock(DatabaseRoutineRepository.class), mapper,
            access, reports, properties);
    private final UUID userId = UUID.randomUUID();
    private final Property property = Property.builder().id(UUID.randomUUID()).active(true).build();
    private final LotIn input = LotIn.builder().lotName("L-1").herdName("  Rebanho Nelore  ").breed(" Nelore ")
            .category(HerdCategory.FATTENING).headCount(200).releaseSlaughterDate(LocalDate.of(2027, 7, 1)).build();

    @Test
    void shouldCreateActiveHerdWithZeroAndFlushLotBeforeSynchronizingReport() throws Exception {
        when(properties.findById(property.getId())).thenReturn(Optional.of(property));
        when(herds.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        service.registerLot(input, userId, property.getId());

        var order = inOrder(access, herds, lots, reports);
        order.verify(access).requireActiveAccess(userId, property.getId());
        order.verify(herds).existsByPropertyId_IdAndHerdName(property.getId(), "Rebanho Nelore");
        order.verify(herds).saveAndFlush(argThat(herd -> herd.isActive() && herd.getHeadCount() == 0
                && herd.getHerdName().equals("Rebanho Nelore") && herd.getBreed().equals("Nelore")
                && herd.getPropertyId() == property));
        order.verify(lots).saveAndFlush(argThat(lot -> lot.getHeadCount() == 200 && lot.isActive()
                && lot.getHerdId().getPropertyId() == property));
        order.verify(reports).saveHerd(userId, property.getId());
    }

    @Test
    void shouldUseOnlyActiveAccessiblePropertyWhenContextIsAbsent() throws Exception {
        var inactive = Property.builder().id(UUID.randomUUID()).active(false).build();
        var ids = List.of(inactive.getId(), property.getId());
        when(access.activePropertyIds(userId)).thenReturn(ids);
        when(properties.findAllById(ids)).thenReturn(List.of(inactive, property));
        when(herds.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        service.registerLot(input, userId, null);

        verify(access).requireActiveAccess(userId, property.getId());
        verify(lots).saveAndFlush(any());
    }

    @Test
    void shouldRejectAmbiguousPropertyWithoutPersisting() {
        var other = Property.builder().id(UUID.randomUUID()).active(true).build();
        var ids = List.of(property.getId(), other.getId());
        when(access.activePropertyIds(userId)).thenReturn(ids);
        when(properties.findAllById(ids)).thenReturn(List.of(property, other));

        assertThatThrownBy(() -> service.registerLot(input, userId, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, error ->
                        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(herds, lots, reports);
    }

    @Test
    void shouldRejectMissingAccessibleProperty() {
        when(access.activePropertyIds(userId)).thenReturn(List.of());
        when(properties.findAllById(List.of())).thenReturn(List.of());

        assertThatThrownBy(() -> service.registerLot(input, userId, null))
                .isInstanceOf(NotFoundException.class).hasMessage("Propriedade não encontrada");
        verifyNoInteractions(herds, lots, reports);
    }

    @Test
    void shouldRejectMissingContextPropertyWithoutFallingBack() {
        when(properties.findById(property.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerLot(input, userId, property.getId()))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(access, herds, lots, reports);
    }

    @Test
    void shouldRejectInactiveContextPropertyWithoutFallingBack() {
        property.setActive(false);
        when(properties.findById(property.getId())).thenReturn(Optional.of(property));

        assertThatThrownBy(() -> service.registerLot(input, userId, property.getId()))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(access, herds, lots, reports);
    }

    @Test
    void shouldRejectUnauthorizedPropertyBeforePersistingHerd() {
        when(properties.findById(property.getId())).thenReturn(Optional.of(property));
        doThrow(new AccessDeniedException("Sem acesso")).when(access).requireActiveAccess(userId, property.getId());

        assertThatThrownBy(() -> service.registerLot(input, userId, property.getId()))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(herds, lots, reports);
    }

    @Test
    void shouldRejectDuplicateNameIncludingInactiveHerds() {
        when(properties.findById(property.getId())).thenReturn(Optional.of(property));
        when(herds.existsByPropertyId_IdAndHerdName(property.getId(), input.herdName())).thenReturn(true);

        assertConflict(() -> service.registerLot(input, userId, property.getId()));
        verify(herds, never()).saveAndFlush(any());
        verifyNoInteractions(lots, reports);
    }

    @Test
    void shouldTranslateConcurrentNameConflictOnly() {
        when(properties.findById(property.getId())).thenReturn(Optional.of(property));
        var failure = new DataIntegrityViolationException("duplicate",
                new ConstraintViolationException("duplicate", new SQLException("duplicate", "23505"), "uq_herd_nome"));
        when(herds.saveAndFlush(any())).thenThrow(failure);

        assertConflict(() -> service.registerLot(input, userId, property.getId()));
        verifyNoInteractions(lots, reports);
    }

    @Test
    void shouldPreserveUnrelatedPersistenceFailure() {
        when(properties.findById(property.getId())).thenReturn(Optional.of(property));
        var failure = new DataIntegrityViolationException("other constraint");
        when(herds.saveAndFlush(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.registerLot(input, userId, property.getId())).isSameAs(failure);
        verifyNoInteractions(lots, reports);
    }

    @Test
    void shouldIgnoreNameBreedAndContextWhenSelectingAuthorizedHerd() throws Exception {
        var herdId = UUID.randomUUID();
        var herd = Herd.builder().id(herdId).herdName("Original").breed("Original breed")
                .headCount(30).propertyId(property).active(true).build();
        when(herds.findWithPropertyByIdAndActiveTrue(herdId)).thenReturn(Optional.of(herd));
        var selected = LotIn.builder().herdId(herdId).herdName("Changed").breed("Changed")
                .lotName("L-1").category(HerdCategory.FATTENING).headCount(5).releaseSlaughterDate(input.releaseSlaughterDate()).build();

        service.registerLot(selected, userId, UUID.randomUUID());

        verify(lots).saveAndFlush(argThat(lot -> lot.getHerdId() == herd && lot.getHeadCount() == 5));
        verify(herds, never()).saveAndFlush(any());
        verifyNoInteractions(properties);
        assertThat(herd.getHerdName()).isEqualTo("Original");
        assertThat(herd.getBreed()).isEqualTo("Original breed");
        assertThat(herd.getHeadCount()).isEqualTo(30);
    }

    @Test
    void shouldRejectUnauthorizedExistingHerd() {
        var herdId = UUID.randomUUID();
        when(herds.findWithPropertyByIdAndActiveTrue(herdId))
                .thenReturn(Optional.of(Herd.builder().propertyId(property).active(true).build()));
        doThrow(new AccessDeniedException("Sem acesso")).when(access).requireActiveAccess(userId, property.getId());

        assertThatThrownBy(() -> service.registerLot(LotIn.builder().herdId(herdId).build(), userId, null))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(lots, reports);
        verify(herds, never()).saveAndFlush(any());
    }

    private void assertConflict(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class, error -> {
            assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(error.getReason()).isEqualTo("Já existe rebanho com esse nome na propriedade");
        });
    }
}
