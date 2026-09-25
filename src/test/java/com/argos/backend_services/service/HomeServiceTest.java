package com.argos.backend_services.service;

import com.argos.backend_services.dto.out.AnimalApplicationOut;
import com.argos.backend_services.dto.out.LotApplicationOut;
import com.argos.backend_services.entity.AnimalException;
import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Application;
import com.argos.backend_services.entity.ApplicationAnimal;
import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.exception.ApplicationWithdrawalRoutineException;
import com.argos.backend_services.mapper.HomeApplicationMapper;
import com.argos.backend_services.repository.ApplicationAnimalRepository;
import com.argos.backend_services.repository.ApplicationRepository;
import com.argos.backend_services.repository.DatabaseRoutineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HomeServiceTest {

    private ApplicationRepository applicationRepository;
    private ApplicationAnimalRepository applicationAnimalRepository;
    private DatabaseRoutineRepository databaseRoutineRepository;
    private HomeApplicationMapper mapper;
    private HomeService service;

    @BeforeEach
    void setUp() {
        applicationRepository = mock(ApplicationRepository.class);
        applicationAnimalRepository = mock(ApplicationAnimalRepository.class);
        databaseRoutineRepository = mock(DatabaseRoutineRepository.class);
        mapper = mock(HomeApplicationMapper.class);
        service = new HomeService(applicationRepository, applicationAnimalRepository, databaseRoutineRepository, mapper);
    }

    @Test
    void shouldReturnLotAndAnimalApplicationsInCalendarWindow() {
        // Arrange
        var userId = UUID.randomUUID();
        var lotId = UUID.randomUUID();
        var application = Application.builder()
                .id(UUID.randomUUID())
                .userId(AppUser.builder().id(userId).build())
                .lotId(Lot.builder().id(lotId).build())
                .build();
        var applicationAnimal = ApplicationAnimal.builder()
                .animalExceptionId(AnimalException.builder().build())
                .build();
        var lotApplication = LotApplicationOut.builder().medicineName("Vacina A").build();
        var animalApplication = AnimalApplicationOut.builder().medicineName("Vacina A").build();
        var referenceDate = LocalDate.now();

        when(applicationRepository.findByUserId_IdAndMeatWithdrawalEndBetween(
                userId, referenceDate.minusDays(7), referenceDate.plusDays(7)))
                .thenReturn(List.of(application));
        when(databaseRoutineRepository.loteAptoAbate(lotId, referenceDate)).thenReturn(false);
        when(mapper.toLotApplicationOut(application, referenceDate, false)).thenReturn(lotApplication);
        when(applicationAnimalRepository.findByApplicationId(application)).thenReturn(List.of(applicationAnimal));
        when(mapper.toAnimalApplicationOut(applicationAnimal, application, referenceDate)).thenReturn(animalApplication);

        // Act
        var result = service.findApplicationCalendar(userId);

        // Assert
        assertThat(result).singleElement().satisfies(homeApplication -> {
            assertThat(homeApplication.getApplicationId()).isEqualTo(application.getId());
            assertThat(homeApplication.getUserId()).isEqualTo(userId);
            assertThat(homeApplication.getLotApplication()).containsExactly(lotApplication);
            assertThat(homeApplication.getAnimalApplication()).containsExactly(animalApplication);
        });
        verify(databaseRoutineRepository).loteAptoAbate(lotId, referenceDate);
    }

    @Test
    void shouldWrapRepositoryFailure() {
        // Arrange
        when(applicationRepository.findByUserId_IdAndMeatWithdrawalEndBetween(any(), any(), any()))
                .thenThrow(new RuntimeException("database unavailable"));

        // Act and Assert
        assertThatThrownBy(() -> service.findApplicationCalendar(UUID.randomUUID()))
                .isInstanceOf(ApplicationWithdrawalRoutineException.class);
    }
}
