package com.argos.backend_services.service;

import com.argos.backend_services.dto.out.ApplicationWithdrawalOut;
import com.argos.backend_services.entity.Application;
import com.argos.backend_services.exception.ApplicationWithdrawalRoutineException;
import com.argos.backend_services.exception.InvalidDateWindowException;
import com.argos.backend_services.mapper.ApplicationWithdrawalMapper;
import com.argos.backend_services.repository.ApplicationRepository;
import com.argos.backend_services.repository.DatabaseRoutineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ApplicationWithdrawalServiceTest {
    private ApplicationRepository applicationRepository;
    private DatabaseRoutineRepository databaseRoutineRepository;
    private ApplicationWithdrawalMapper mapper;
    private ApplicationWithdrawalService service;

    @BeforeEach
    void setUp() {
        applicationRepository = mock(ApplicationRepository.class);
        databaseRoutineRepository = mock(DatabaseRoutineRepository.class);
        mapper = mock(ApplicationWithdrawalMapper.class);
        service = new ApplicationWithdrawalService(applicationRepository, databaseRoutineRepository, mapper);
    }

    @Test
    void shouldMapApplicationsInConfiguredDateWindow() {
        var referenceDate = LocalDate.of(2026, 9, 20);
        var application = Application.builder().id(UUID.randomUUID()).build();
        var expected = ApplicationWithdrawalOut.builder().applicationId(application.getId()).build();
        when(applicationRepository.findByMeatWithdrawalEndBetween(referenceDate.minusDays(7), referenceDate.plusDays(7)))
                .thenReturn(List.of(application));
        when(mapper.toOut(application, referenceDate)).thenReturn(expected);

        assertThat(service.findNearDate(referenceDate, 7, 7)).containsExactly(expected);
        verify(mapper).toOut(application, referenceDate);
    }

    @Test
    void shouldRejectNegativeDateWindow() {
        assertThatThrownBy(() -> service.findNearDate(LocalDate.now(), -1, 7))
                .isInstanceOf(InvalidDateWindowException.class);
        verifyNoInteractions(applicationRepository, mapper);
    }

    @Test
    void shouldWrapRepositoryFailure() {
        when(applicationRepository.findByMeatWithdrawalEndBetween(any(), any()))
                .thenThrow(new RuntimeException("database unavailable"));

        assertThatThrownBy(() -> service.findNearDate(LocalDate.now(), 7, 7))
                .isInstanceOf(ApplicationWithdrawalRoutineException.class);
    }
}
