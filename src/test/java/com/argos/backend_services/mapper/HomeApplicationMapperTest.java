package com.argos.backend_services.mapper;

import com.argos.backend_services.entity.AnimalException;
import com.argos.backend_services.entity.Application;
import com.argos.backend_services.entity.ApplicationAnimal;
import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.entity.Medicine;
import com.argos.backend_services.entity.MedicineBatch;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HomeApplicationMapperTest {

    private final HomeApplicationMapper mapper = Mappers.getMapper(HomeApplicationMapper.class);

    @Test
    void shouldMapLotApplicationCalendarData() {
        // Arrange
        var referenceDate = LocalDate.of(2026, 9, 21);
        var application = application(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 25));

        // Act
        var result = mapper.toLotApplicationOut(application, referenceDate, false);

        // Assert
        assertThat(result.getMedicineName()).isEqualTo("Vacina A");
        assertThat(result.getApplicationDate()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(result.getMeatWithdrawalEnd()).isEqualTo(LocalDate.of(2026, 9, 25));
        assertThat(result.getDaysFromReferenceDate()).isEqualTo(4);
        assertThat(result.getDaysUntilSlaughter()).isEqualTo(4);
    }

    @Test
    void shouldMapAnimalApplicationCalendarData() {
        // Arrange
        var referenceDate = LocalDate.of(2026, 9, 21);
        var application = application(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 25), null);
        var applicationAnimal = ApplicationAnimal.builder()
                .meatWithdrawalDate(LocalDate.of(2026, 9, 23))
                .animalExceptionId(AnimalException.builder()
                        .slaughterReleaseDate(LocalDate.of(2026, 9, 24))
                        .build())
                .releasedForSlaughter(false)
                .build();

        // Act
        var result = mapper.toAnimalApplicationOut(applicationAnimal, application, referenceDate);

        // Assert
        assertThat(result.getMedicineName()).isEqualTo("Vacina A");
        assertThat(result.getApplicationDate()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(result.getMeatWithdrawalEnd()).isEqualTo(LocalDate.of(2026, 9, 23));
        assertThat(result.getDaysFromReferenceDate()).isEqualTo(2);
        assertThat(result.getDaysUntilSlaughter()).isEqualTo(3);
    }

    private Application application(LocalDate applicationDate, LocalDate withdrawalEnd, LocalDate slaughterReleaseDate) {
        return Application.builder()
                .applicationDate(applicationDate)
                .meatWithdrawalEnd(withdrawalEnd)
                .lotId(Lot.builder().slaughterReleaseDate(slaughterReleaseDate).build())
                .medicineBatchId(MedicineBatch.builder()
                        .medicineId(Medicine.builder().brandName("Vacina A").build())
                        .build())
                .build();
    }
}
