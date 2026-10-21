package com.argos.backend_services.service;

import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.entity.HerdReport;
import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.entity.AnimalException;
import com.argos.backend_services.repository.AnimalExceptionRepository;
import com.argos.backend_services.repository.ApplicationAnimalRepository;
import com.argos.backend_services.repository.ApplicationRepository;
import com.argos.backend_services.repository.HerdReportRepository;
import com.argos.backend_services.repository.HerdRepository;
import com.argos.backend_services.repository.LotRepository;
import com.argos.backend_services.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportHerdServiceTest {

    @Test
    void shouldCreateReportWithHerdAndLotData() {
        var propertyRepository = mock(PropertyRepository.class);
        var herdRepository = mock(HerdRepository.class);
        var reportRepository = mock(HerdReportRepository.class);
        var lotRepository = mock(LotRepository.class);
        var applicationRepository = mock(ApplicationRepository.class);
        var animalExceptionRepository = mock(AnimalExceptionRepository.class);
        var applicationAnimalRepository = mock(ApplicationAnimalRepository.class);
        var propertyAccessService = mock(PropertyAccessService.class);
        var property = Property.builder().id(UUID.randomUUID()).name("Fazenda Boa Vista").build();
        var herd = Herd.builder().id(UUID.randomUUID()).propertyId(property).herdName("Nelore").breed("Nelore").build();
        var lot = Lot.builder().id(UUID.randomUUID()).herdId(herd).lotCode("L-01").category("Engorda")
                .headCount(25).openDate(LocalDate.of(2026, 1, 10)).active(true).build();
        var openAnimal = AnimalException.builder().id(UUID.randomUUID()).lotId(lot).originLotId(lot)
                .exceptionDate(LocalDate.of(2026, 2, 1)).status(AnimalException.StatusAnimal.ATIVO).build();
        var closedAnimal = AnimalException.builder().id(UUID.randomUUID()).lotId(lot).originLotId(lot)
                .exceptionDate(LocalDate.of(2026, 2, 2)).status(AnimalException.StatusAnimal.RECUPERADO).build();
        var service = service(propertyRepository, herdRepository, reportRepository, lotRepository,
                applicationRepository, animalExceptionRepository, applicationAnimalRepository, propertyAccessService);
        var userId = UUID.randomUUID();

        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(herdRepository.findByPropertyIdAndActiveTrue(property)).thenReturn(List.of(herd));
        when(lotRepository.findByHerdId(herd)).thenReturn(List.of(lot));
        when(applicationRepository.findByLotIdIn(List.of(lot))).thenReturn(List.of());
        when(animalExceptionRepository.findByLotIdIn(List.of(lot))).thenReturn(List.of(openAnimal, closedAnimal));
        when(applicationAnimalRepository.findByAnimalExceptionIdIn(List.of(openAnimal, closedAnimal))).thenReturn(List.of());
        when(reportRepository.findByHerd_Id(herd.getId().toString())).thenReturn(Optional.empty());

        service.saveHerd(userId, property.getId());

        var reportCaptor = ArgumentCaptor.forClass(HerdReport.class);
        verify(propertyAccessService).requireActiveAccess(userId, property.getId());
        verify(reportRepository).save(reportCaptor.capture());
        var report = reportCaptor.getValue();
        assertThat(report.getReportType()).isEqualTo("ON_DEMAND");
        assertThat(report.getRelationalPropertyId()).isEqualTo(property.getId().toString());
        assertThat(report.getPropertyName()).isEqualTo("Fazenda Boa Vista");
        assertThat(report.getHerd().getId()).isEqualTo(herd.getId().toString());
        assertThat(report.getLots()).singleElement().satisfies(savedLot -> {
            assertThat(savedLot.getId()).isEqualTo(lot.getId().toString());
            assertThat(savedLot.getCode()).isEqualTo("L-01");
            assertThat(savedLot.getHeadCount()).isEqualTo(25);
        });
        assertThat(report.getHistorySummary().getTotalLots()).isEqualTo(1);
        assertThat(report.getHistorySummary().getTotalOccurrences()).isEqualTo(2);
        assertThat(report.getHistorySummary().getOpenOccurrenceCount()).isEqualTo(1);
    }

    @Test
    void shouldUpdateExistingReportDuringDailySynchronization() {
        var propertyRepository = mock(PropertyRepository.class);
        var herdRepository = mock(HerdRepository.class);
        var reportRepository = mock(HerdReportRepository.class);
        var lotRepository = mock(LotRepository.class);
        var applicationRepository = mock(ApplicationRepository.class);
        var animalExceptionRepository = mock(AnimalExceptionRepository.class);
        var applicationAnimalRepository = mock(ApplicationAnimalRepository.class);
        var propertyAccessService = mock(PropertyAccessService.class);
        var property = Property.builder().id(UUID.randomUUID()).name("Fazenda Boa Vista").build();
        var herd = Herd.builder().id(UUID.randomUUID()).propertyId(property).herdName("Nelore").breed("Nelore").build();
        var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        var existingReport = HerdReport.builder().id("existing-report").reportCreatedAt(createdAt).build();
        var service = service(propertyRepository, herdRepository, reportRepository, lotRepository,
                applicationRepository, animalExceptionRepository, applicationAnimalRepository, propertyAccessService);

        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(herdRepository.findByPropertyIdAndActiveTrue(property)).thenReturn(List.of(herd));
        when(lotRepository.findByHerdId(herd)).thenReturn(List.of());
        when(reportRepository.findByHerd_Id(herd.getId().toString())).thenReturn(Optional.of(existingReport));

        service.syncHerd(UUID.randomUUID(), property.getId());

        var reportCaptor = ArgumentCaptor.forClass(HerdReport.class);
        verify(reportRepository).save(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getId()).isEqualTo("existing-report");
        assertThat(reportCaptor.getValue().getReportCreatedAt()).isEqualTo(createdAt);
        assertThat(reportCaptor.getValue().getReportType()).isEqualTo("DAILY_SYNC");
    }

    private ReportHerdService service(PropertyRepository propertyRepository,
                                      HerdRepository herdRepository,
                                      HerdReportRepository reportRepository,
                                      LotRepository lotRepository,
                                      ApplicationRepository applicationRepository,
                                      AnimalExceptionRepository animalExceptionRepository,
                                      ApplicationAnimalRepository applicationAnimalRepository,
                                      PropertyAccessService propertyAccessService) {
        return new ReportHerdService(
                propertyRepository,
                herdRepository,
                reportRepository,
                lotRepository,
                applicationRepository,
                animalExceptionRepository,
                applicationAnimalRepository,
                propertyAccessService);
    }
}
