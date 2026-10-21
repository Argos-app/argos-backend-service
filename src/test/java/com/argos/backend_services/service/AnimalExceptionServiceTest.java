package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.AnimalExceptionIn;
import com.argos.backend_services.dto.out.AnimalExceptionOut;
import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.mapper.AnimalExceptionMapper;
import com.argos.backend_services.repository.AnimalExceptionRepository;
import com.argos.backend_services.repository.ApplicationAnimalRepository;
import com.argos.backend_services.repository.DatabaseRoutineRepository;
import com.argos.backend_services.repository.LotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnimalExceptionServiceTest {

    private AnimalExceptionRepository animalExceptionRepository;
    private ApplicationAnimalRepository applicationAnimalRepository;
    private LotRepository lotRepository;
    private PropertyAccessService propertyAccessService;
    private DatabaseRoutineRepository databaseRoutineRepository;
    private AnimalExceptionService service;

    @BeforeEach
    void setUp() {
        animalExceptionRepository = mock(AnimalExceptionRepository.class);
        applicationAnimalRepository = mock(ApplicationAnimalRepository.class);
        lotRepository = mock(LotRepository.class);
        propertyAccessService = mock(PropertyAccessService.class);
        databaseRoutineRepository = mock(DatabaseRoutineRepository.class);
        service = new AnimalExceptionService(
                animalExceptionRepository,
                applicationAnimalRepository,
                lotRepository,
                propertyAccessService,
                databaseRoutineRepository,
                mock(AnimalExceptionMapper.class));
    }

    @Test
    void shouldRegisterExceptionUsingExistingRoutine() throws Exception {
        var propertyId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var originLot = lot("ORIG-01", true, propertyId);
        var input = new AnimalExceptionIn(originLot.getId(), "BR-001", "Doença respiratória", new BigDecimal("420.50"));
        when(lotRepository.findWithHerdAndPropertyById(originLot.getId())).thenReturn(Optional.of(originLot));

        service.register(input, userId);

        verify(propertyAccessService).requireActiveAccess(userId, propertyId);
        verify(databaseRoutineRepository).registerException(
                originLot.getId(), input.earTag(), input.exceptionReason(), input.currentWeight());
    }

    private Lot lot(String lotCode, boolean active, UUID propertyId) {
        return Lot.builder()
                .id(UUID.randomUUID())
                .lotCode(lotCode)
                .active(active)
                .herdId(Herd.builder().propertyId(Property.builder().id(propertyId).build()).build())
                .build();
    }
}
