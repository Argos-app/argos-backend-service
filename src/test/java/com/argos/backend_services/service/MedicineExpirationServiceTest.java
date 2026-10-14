package com.argos.backend_services.service;

import com.argos.backend_services.repository.MedicineBatchRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MedicineExpirationServiceTest {

    @Test
    void shouldMarkOnlyAvailableBatchesExpiredBeforeToday() {
        var repository = mock(MedicineBatchRepository.class);
        var clock = Clock.fixed(Instant.parse("2026-10-11T12:00:00Z"), ZoneOffset.UTC);
        var service = new MedicineExpirationService(repository, clock);
        when(repository.markAvailableBatchesExpired(LocalDate.of(2026, 10, 11))).thenReturn(3);

        assertThat(service.markExpiredBatches()).isEqualTo(3);
        verify(repository).markAvailableBatchesExpired(LocalDate.of(2026, 10, 11));
    }
}
