package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.AnimalApplicationIn;
import com.argos.backend_services.entity.AnimalException;
import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.entity.Medicine;
import com.argos.backend_services.entity.MedicineBatch;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.repository.AnimalExceptionRepository;
import com.argos.backend_services.repository.DatabaseRoutineRepository;
import com.argos.backend_services.repository.MedicineBatchRepository;
import com.argos.backend_services.repository.PropertyRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnimalApplicationServiceTest {

    @Test
    void shouldReturnAnimalWhenWithdrawalHasEndedAndOriginLotIsActive() throws Exception {
        var exceptionRepository = mock(AnimalExceptionRepository.class);
        var medicineBatchRepository = mock(MedicineBatchRepository.class);
        var propertyRepository = mock(PropertyRepository.class);
        var routines = mock(DatabaseRoutineRepository.class);
        var service = new AnimalApplicationService(
                exceptionRepository, medicineBatchRepository, propertyRepository, routines);
        var propertyId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var animalExceptionId = UUID.randomUUID();
        var medicine = Medicine.builder().id(UUID.randomUUID()).build();
        var medicineBatch = MedicineBatch.builder().id(UUID.randomUUID()).medicineId(medicine).build();
        var originLot = Lot.builder().active(true)
                .herdId(Herd.builder().propertyId(Property.builder().id(propertyId).build()).build())
                .build();
        var animalException = AnimalException.builder().id(animalExceptionId).originLotId(originLot)
                .status(AnimalException.StatusAnimal.ATIVO).build();
        var input = new AnimalApplicationIn(medicineBatch.getId(), new BigDecimal("5"), "ml", "ORAL", "Tratamento");

        when(exceptionRepository.findWithLotsById(animalExceptionId)).thenReturn(Optional.of(animalException));
        when(propertyRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(Property.builder().id(propertyId).build()));
        when(medicineBatchRepository.findById(medicineBatch.getId())).thenReturn(Optional.of(medicineBatch));
        when(routines.calculateSlaughterDate(medicine.getId(), LocalDate.now())).thenReturn(LocalDate.now());

        service.register(animalExceptionId, input, userId);

        verify(routines).registerAnimalApplication(
                animalExceptionId, medicineBatch.getId(), userId, input.dose(), input.unit(), input.route(), input.goal());
        verify(routines).returnException(animalExceptionId);
    }
}
