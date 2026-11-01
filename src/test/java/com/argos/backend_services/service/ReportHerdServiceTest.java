package com.argos.backend_services.service;

import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.entity.HerdReport;
import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.exception.PropertyNotFoundException;
import com.argos.backend_services.entity.AnimalException;
import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Permission;
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
    void shouldCreateReportWithHerdAndLotData() throws PropertyNotFoundException {
        var propertyRepository = mock(PropertyRepository.class);
        var herdRepository = mock(HerdRepository.class);
        var reportRepository = mock(HerdReportRepository.class);
        var lotRepository = mock(LotRepository.class);
        var applicationRepository = mock(ApplicationRepository.class);
        var animalExceptionRepository = mock(AnimalExceptionRepository.class);
        var applicationAnimalRepository = mock(ApplicationAnimalRepository.class);
        var propertyAccessService = mock(PropertyAccessService.class);
        var property = property("Fazenda Boa Vista");
        var herd = herd(property);
        var lot = lot(herd);
        var openAnimal = animal(lot, LocalDate.of(2026, 2, 1), AnimalException.StatusAnimal.ATIVO);
        var closedAnimal = animal(lot, LocalDate.of(2026, 2, 2), AnimalException.StatusAnimal.RECUPERADO);
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
    void shouldUpdateExistingReportDuringDailySynchronization() throws PropertyNotFoundException {
        var propertyRepository = mock(PropertyRepository.class);
        var herdRepository = mock(HerdRepository.class);
        var reportRepository = mock(HerdReportRepository.class);
        var lotRepository = mock(LotRepository.class);
        var applicationRepository = mock(ApplicationRepository.class);
        var animalExceptionRepository = mock(AnimalExceptionRepository.class);
        var applicationAnimalRepository = mock(ApplicationAnimalRepository.class);
        var propertyAccessService = mock(PropertyAccessService.class);
        var property = property("Fazenda Boa Vista");
        var herd = herd(property);
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

    private Property property(String name) {
        var now = LocalDateTime.of(2026, 1, 1, 10, 0);
        var permission = Permission.builder().id(UUID.randomUUID()).code("USER").name("User")
                .active(true).build();
        var user = AppUser.builder().id(UUID.randomUUID()).permission(permission).name("Test User")
                .cpf("12345678901").email("user@test.invalid").firstLogin(false).active(true)
                .createdAt(now).updatedAt(now).build();
        return Property.builder().id(UUID.randomUUID()).user(user).name(name).address("Test Street, 1")
                .city("Test City").state("SP").active(true).createdAt(now).build();
    }

    private Herd herd(Property property) {
        var now = LocalDateTime.of(2026, 1, 1, 10, 0);
        return Herd.builder().id(UUID.randomUUID()).propertyId(property).herdName("Nelore").breed("Nelore")
                .headCount(25).active(true).createdAt(now).updatedAt(now).build();
    }

    private Lot lot(Herd herd) {
        var now = LocalDateTime.of(2026, 1, 10, 10, 0);
        return Lot.builder().id(UUID.randomUUID()).herdId(herd).lotCode("L-01").category("Engorda")
                .isExceptionLot(false).headCount(25).fitForSlaughter(true).openDate(now.toLocalDate())
                .active(true).createdAt(now).updatedAt(now).build();
    }

    private AnimalException animal(Lot lot, LocalDate exceptionDate, AnimalException.StatusAnimal status) {
        var now = LocalDateTime.of(2026, 2, 1, 10, 0);
        return AnimalException.builder().id(UUID.randomUUID()).lotId(lot).originLotId(lot).earTag("BR-001")
                .category("BOI").exceptionDate(exceptionDate).exceptionReason("Teste").fitForSlaughter(true)
                .status(status).createdAt(now).updatedAt(now).build();
    }
}
