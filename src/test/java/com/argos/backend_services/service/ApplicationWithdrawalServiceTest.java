package com.argos.backend_services.service;

import com.argos.backend_services.dto.out.ApplicationWithdrawalOut;
import com.argos.backend_services.entity.Application;
import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.exception.ApplicationWithdrawalRoutineException;
import com.argos.backend_services.exception.InvalidDateWindowException;
import com.argos.backend_services.mapper.ApplicationWithdrawalMapper;
import com.argos.backend_services.repository.ApplicationRepository;
import com.argos.backend_services.repository.DatabaseRoutineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ApplicationWithdrawalServiceTest {
    private ApplicationRepository applicationRepository;
    private DatabaseRoutineRepository databaseRoutineRepository;
    private ApplicationWithdrawalMapper mapper;
    private PropertyAccessService propertyAccessService;
    private ApplicationWithdrawalService service;

    @BeforeEach
    void setUp() {
        applicationRepository = mock(ApplicationRepository.class);
        databaseRoutineRepository = mock(DatabaseRoutineRepository.class);
        mapper = mock(ApplicationWithdrawalMapper.class);
        propertyAccessService = mock(PropertyAccessService.class);
        service = new ApplicationWithdrawalService(applicationRepository, databaseRoutineRepository, mapper,
                propertyAccessService, Clock.fixed(Instant.parse("2026-10-26T11:00:00Z"), ZoneId.of("America/Sao_Paulo")));
    }

    @Test
    void shouldMapApplicationsInConfiguredDateWindow() throws InvalidDateWindowException {
        var referenceDate = LocalDate.of(2026, 9, 20);
        var userId = UUID.randomUUID();
        var propertyIds = List.of(UUID.randomUUID());
        var application = Application.builder().id(UUID.randomUUID()).build();
        var expected = ApplicationWithdrawalOut.builder().applicationId(application.getId()).build();
        when(propertyAccessService.activePropertyIds(userId)).thenReturn(propertyIds);
        when(applicationRepository.findByMeatWithdrawalEndBetweenAndLotId_HerdId_PropertyId_IdIn(referenceDate.minusDays(7), referenceDate.plusDays(7), propertyIds))
                .thenReturn(List.of(application));
        when(mapper.toOut(application, referenceDate)).thenReturn(expected);

        assertThat(service.findNearDate(referenceDate, 7, 7, userId)).containsExactly(expected);
        verify(mapper).toOut(application, referenceDate);
    }

    @Test
    void shouldRejectNegativeDateWindow() {
        assertThatThrownBy(() -> service.findNearDate(LocalDate.now(), -1, 7, UUID.randomUUID()))
                .isInstanceOf(InvalidDateWindowException.class);
        verifyNoInteractions(applicationRepository, mapper);
    }

    @Test
    void shouldWrapRepositoryFailure() {
        var userId = UUID.randomUUID();
        when(propertyAccessService.activePropertyIds(userId)).thenReturn(List.of(UUID.randomUUID()));
        when(applicationRepository.findByMeatWithdrawalEndBetweenAndLotId_HerdId_PropertyId_IdIn(any(), any(), any()))
                .thenThrow(new RuntimeException("database unavailable"));

        assertThatThrownBy(() -> service.findNearDate(LocalDate.now(), 7, 7, userId))
                .isInstanceOf(ApplicationWithdrawalRoutineException.class);
    }

    @Test
    void shouldReturnEmptyWindowWhenThereAreNoApplications() throws InvalidDateWindowException {
        var referenceDate = LocalDate.of(2026, 9, 20);
        var userId = UUID.randomUUID();
        var propertyIds = List.of(UUID.randomUUID());
        when(propertyAccessService.activePropertyIds(userId)).thenReturn(propertyIds);
        when(applicationRepository.findByMeatWithdrawalEndBetweenAndLotId_HerdId_PropertyId_IdIn(referenceDate.minusDays(7), referenceDate.plusDays(7), propertyIds))
                .thenReturn(List.of());

        assertThat(service.findNearDate(referenceDate, 7, 7, userId)).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldUseSaoPauloDateAndOnlyReleaseEligibleLots() {
        var today = LocalDate.of(2026, 10, 26);
        var eligibleLot = Lot.builder().id(UUID.randomUUID()).build();
        var blockedLot = Lot.builder().id(UUID.randomUUID()).build();
        var eligible = Application.builder().id(UUID.randomUUID()).lotId(eligibleLot).build();
        var blocked = Application.builder().id(UUID.randomUUID()).lotId(blockedLot).build();
        var expected = ApplicationWithdrawalOut.builder().applicationId(eligible.getId()).build();
        when(applicationRepository.findByMeatWithdrawalEndBetween(today, today))
                .thenReturn(List.of(eligible, blocked));
        when(databaseRoutineRepository.lotIsReleaseForSlaughter(eligibleLot.getId(), today)).thenReturn(true);
        when(databaseRoutineRepository.lotIsReleaseForSlaughter(blockedLot.getId(), today)).thenReturn(false);
        when(mapper.toOut(eligible, today)).thenReturn(expected);

        assertThat(service.findDueToday()).containsExactly(expected);
        verify(mapper, never()).toOut(eq(blocked), any());
    }
}
