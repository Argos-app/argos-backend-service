package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.ApplicationIn;
import com.argos.backend_services.dto.out.ApplicationOut;
import com.argos.backend_services.entity.Application;
import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.entity.Medicine;
import com.argos.backend_services.entity.MedicineBatch;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.exception.MedicineNotFoundException;
import com.argos.backend_services.mapper.ApplicationMapper;
import com.argos.backend_services.repository.ApplicationRepository;
import com.argos.backend_services.repository.ApplicationAnimalRepository;
import com.argos.backend_services.repository.DatabaseRoutineRepository;
import com.argos.backend_services.repository.LotRepository;
import com.argos.backend_services.repository.MedicineBatchRepository;
import com.argos.backend_services.repository.MedicineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationServiceTest {

    private LotRepository lotRepository;
    private ApplicationRepository applicationRepository;
    private MedicineBatchRepository medicineBatchRepository;
    private MedicineRepository medicineRepository;
    private DatabaseRoutineRepository databaseRoutineRepository;
    private ApplicationAnimalRepository applicationAnimalRepository;
    private ApplicationMapper mapper;
    private PropertyAccessService propertyAccessService;
    private ApplicationService service;

    @BeforeEach
    void setUp() {
        lotRepository = mock(LotRepository.class);
        applicationRepository = mock(ApplicationRepository.class);
        medicineBatchRepository = mock(MedicineBatchRepository.class);
        medicineRepository = mock(MedicineRepository.class);
        databaseRoutineRepository = mock(DatabaseRoutineRepository.class);
        applicationAnimalRepository = mock(ApplicationAnimalRepository.class);
        mapper = mock(ApplicationMapper.class);
        propertyAccessService = mock(PropertyAccessService.class);
        service = new ApplicationService(lotRepository, applicationRepository,
                medicineBatchRepository, medicineRepository, databaseRoutineRepository,
                applicationAnimalRepository, mapper, propertyAccessService);
    }

    @Test
    void shouldRegisterApplicationUsingLotMedicineAndBatchIds() throws Exception {
        UUID lotId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        Lot lot = Lot.builder().id(lotId).lotCode("LOTE-001")
                .herdId(Herd.builder().propertyId(Property.builder().id(propertyId).build()).build()).build();
        Medicine medicine = Medicine.builder().id(medicineId).brandName("Ivermectina").build();
        MedicineBatch batch = MedicineBatch.builder().id(batchId).medicineId(medicine).build();
        ApplicationIn input = ApplicationIn.builder()
                .lotName("LOTE-001").medicineName("Ivermectina")
                .dose(new BigDecimal("10.5")).unit("ML").route("ORAL")
                .goal("Tratamento").build();

        when(lotRepository.findWithHerdAndPropertyByLotCode("LOTE-001")).thenReturn(Optional.of(lot));
        when(medicineRepository.findByBrandName("Ivermectina")).thenReturn(Optional.of(medicine));
        when(medicineBatchRepository.findByMedicineId(medicine)).thenReturn(Optional.of(batch));

        service.registerApplication(input, userId);

        verify(databaseRoutineRepository).registerApplication(
                lotId, batchId, userId, input.dose(), input.unit(), input.route(), input.goal());
    }

    @Test
    void shouldThrowWhenLotDoesNotExist() {
        ApplicationIn input = ApplicationIn.builder().lotName("INEXISTENTE").build();
        when(lotRepository.findWithHerdAndPropertyByLotCode("INEXISTENTE")).thenReturn(Optional.empty());

        assertThrows(LotNotFoundException.class, () -> service.registerApplication(input, UUID.randomUUID()));
        verifyNoInteractions(medicineRepository, medicineBatchRepository, databaseRoutineRepository);
    }

    @Test
    void shouldThrowWhenMedicineDoesNotExist() {
        ApplicationIn input = ApplicationIn.builder().lotName("LOTE-001").medicineName("Inexistente").build();
        var propertyId = UUID.randomUUID();
        when(lotRepository.findWithHerdAndPropertyByLotCode("LOTE-001"))
                .thenReturn(Optional.of(Lot.builder().id(UUID.randomUUID())
                        .herdId(Herd.builder().propertyId(Property.builder().id(propertyId).build()).build()).build()));
        when(medicineRepository.findByBrandName("Inexistente")).thenReturn(Optional.empty());

        assertThrows(MedicineNotFoundException.class, () -> service.registerApplication(input, UUID.randomUUID()));
        verifyNoInteractions(medicineBatchRepository, databaseRoutineRepository);
    }

    @Test
    void shouldListApplicationsUsingMapper() {
        List<Application> entities = List.of(Application.builder().build());
        List<ApplicationOut> expected = List.of(ApplicationOut.builder().lotName("LOTE-001").build());
        var userId = UUID.randomUUID();
        var propertyIds = List.of(UUID.randomUUID());
        when(propertyAccessService.activePropertyIds(userId)).thenReturn(propertyIds);
        when(applicationRepository.findByLotId_HerdId_PropertyId_IdIn(propertyIds)).thenReturn(entities);
        when(mapper.listToOut(entities)).thenReturn(expected);

        assertSame(expected, service.listApplication(userId));
        verify(mapper).listToOut(entities);
    }
}
