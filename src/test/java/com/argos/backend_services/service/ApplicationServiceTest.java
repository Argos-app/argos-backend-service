
package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.ApplicationIn;
import com.argos.backend_services.dto.out.ApplicationOut;
import com.argos.backend_services.entity.*;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.mapper.ApplicationMapper;
import com.argos.backend_services.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

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

        service = new ApplicationService(
                lotRepository,
                applicationRepository,
                medicineBatchRepository,
                medicineRepository,
                databaseRoutineRepository,
                applicationAnimalRepository,
                mapper,
                propertyAccessService
        );
    }

    @Test
    void shouldRegisterApplicationUsingLotIdAndMedicineBatchId() throws Exception {
        UUID lotId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();

        Lot lot = Lot.builder()
                .id(lotId)
                .lotCode("LOTE-001")
                .herdId(Herd.builder()
                        .propertyId(Property.builder()
                                .id(propertyId)
                                .build())
                        .build())
                .build();

        ApplicationIn input = ApplicationIn.builder()
                .lotId(lotId)
                .lotName("LOTE-001")
                .medicineBatchId(batchId)
                .dose(new BigDecimal("10.5"))
                .unit("ML")
                .route("ORAL")
                .goal("Tratamento")
                .build();

        when(lotRepository.findWithHerdAndPropertyById(lotId))
                .thenReturn(Optional.of(lot));

        service.registerApplication(input, userId);

        InOrder order = inOrder(
                lotRepository,
                propertyAccessService,
                databaseRoutineRepository
        );

        order.verify(lotRepository)
                .findWithHerdAndPropertyById(lotId);

        order.verify(propertyAccessService)
                .requireActiveAccess(userId, propertyId);

        order.verify(databaseRoutineRepository).registerApplication(
                lotId,
                batchId,
                userId,
                input.dose(),
                input.unit(),
                input.route(),
                input.goal()
        );

        verify(lotRepository, never())
                .findWithHerdAndPropertyByLotCode(anyString());

        verifyNoInteractions(
                medicineRepository,
                medicineBatchRepository
        );
    }

    @Test
    void shouldRegisterApplicationUsingLotCodeWhenLotIdIsNull() throws Exception {
        UUID lotId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();

        Lot lot = Lot.builder()
                .id(lotId)
                .lotCode("LOTE-001")
                .herdId(Herd.builder()
                        .propertyId(Property.builder()
                                .id(propertyId)
                                .build())
                        .build())
                .build();

        ApplicationIn input = ApplicationIn.builder()
                .lotName("LOTE-001")
                .medicineBatchId(batchId)
                .dose(new BigDecimal("5.0"))
                .unit("ML")
                .route("ORAL")
                .goal("Prevenção")
                .build();

        when(lotRepository.findWithHerdAndPropertyByLotCode("LOTE-001"))
                .thenReturn(Optional.of(lot));

        service.registerApplication(input, userId);

        verify(lotRepository)
                .findWithHerdAndPropertyByLotCode("LOTE-001");

        verify(lotRepository, never())
                .findWithHerdAndPropertyById(any());

        verify(propertyAccessService)
                .requireActiveAccess(userId, propertyId);

        verify(databaseRoutineRepository).registerApplication(
                lotId,
                batchId,
                userId,
                input.dose(),
                input.unit(),
                input.route(),
                input.goal()
        );
    }

    @Test
    void shouldThrowWhenLotDoesNotExist() {
        UUID userId = UUID.randomUUID();

        ApplicationIn input = ApplicationIn.builder()
                .lotName("INEXISTENTE")
                .build();

        when(lotRepository.findWithHerdAndPropertyByLotCode("INEXISTENTE"))
                .thenReturn(Optional.empty());

        LotNotFoundException exception = assertThrows(
                LotNotFoundException.class,
                () -> service.registerApplication(input, userId)
        );

        assertEquals(
                "Não foi possível achar o lote informado",
                exception.getMessage()
        );

        verify(lotRepository)
                .findWithHerdAndPropertyByLotCode("INEXISTENTE");

        verifyNoInteractions(
                propertyAccessService,
                medicineRepository,
                medicineBatchRepository,
                databaseRoutineRepository
        );
    }

    @Test
    void shouldThrowWhenLotIdDoesNotExist() {
        UUID lotId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ApplicationIn input = ApplicationIn.builder()
                .lotId(lotId)
                .build();

        when(lotRepository.findWithHerdAndPropertyById(lotId))
                .thenReturn(Optional.empty());

        assertThrows(
                LotNotFoundException.class,
                () -> service.registerApplication(input, userId)
        );

        verify(lotRepository)
                .findWithHerdAndPropertyById(lotId);

        verifyNoInteractions(
                propertyAccessService,
                databaseRoutineRepository
        );
    }

    @Test
    void shouldNotRegisterApplicationWhenUserHasNoPropertyAccess() {
        UUID lotId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();

        Lot lot = Lot.builder()
                .id(lotId)
                .herdId(Herd.builder()
                        .propertyId(Property.builder()
                                .id(propertyId)
                                .build())
                        .build())
                .build();

        ApplicationIn input = ApplicationIn.builder()
                .lotId(lotId)
                .medicineBatchId(batchId)
                .dose(new BigDecimal("10.5"))
                .unit("ML")
                .route("ORAL")
                .goal("Tratamento")
                .build();

        when(lotRepository.findWithHerdAndPropertyById(lotId))
                .thenReturn(Optional.of(lot));

        doThrow(new SecurityException("Acesso negado"))
                .when(propertyAccessService)
                .requireActiveAccess(userId, propertyId);

        assertThrows(
                SecurityException.class,
                () -> service.registerApplication(input, userId)
        );

        verify(propertyAccessService)
                .requireActiveAccess(userId, propertyId);

        verifyNoInteractions(databaseRoutineRepository);
    }

    @Test
    void shouldListApplicationsUsingMapper() {
        List<Application> entities = List.of(
                Application.builder().build()
        );

        List<ApplicationOut> expected = List.of(
                ApplicationOut.builder()
                        .lotName("LOTE-001")
                        .build()
        );

        UUID userId = UUID.randomUUID();
        List<UUID> propertyIds = List.of(UUID.randomUUID());

        when(propertyAccessService.activePropertyIds(userId))
                .thenReturn(propertyIds);

        when(applicationRepository.findByLotId_HerdId_PropertyId_IdIn(propertyIds))
                .thenReturn(entities);

        when(mapper.listToOut(entities))
                .thenReturn(expected);

        assertSame(expected, service.listApplication(userId));

        verify(mapper).listToOut(entities);
    }
}