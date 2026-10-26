package com.argos.backend_services.controller;

import com.argos.backend_services.config.SecurityConfig;
import com.argos.backend_services.config.WebConfig;
import com.argos.backend_services.controller.handler.CustomGlobalException;
import com.argos.backend_services.controller.impl.AnimalApplicationController;
import com.argos.backend_services.controller.impl.AnimalExceptionController;
import com.argos.backend_services.controller.resolver.CurrentUserArgumentResolver;
import com.argos.backend_services.controller.resolver.FirebaseTokenArgumentResolver;
import com.argos.backend_services.dto.out.AnimalExceptionOut;
import com.argos.backend_services.entity.AnimalException;
import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.entity.Medicine;
import com.argos.backend_services.entity.MedicineBatch;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.mapper.AnimalExceptionMapper;
import com.argos.backend_services.repository.AnimalExceptionRepository;
import com.argos.backend_services.repository.ApplicationAnimalRepository;
import com.argos.backend_services.repository.DatabaseRoutineRepository;
import com.argos.backend_services.repository.LotRepository;
import com.argos.backend_services.repository.MedicineBatchRepository;
import com.argos.backend_services.repository.UserRepository;
import com.argos.backend_services.security.CurrentUserData;
import com.argos.backend_services.security.JwtAuthenticationFilter;
import com.argos.backend_services.service.AnimalApplicationService;
import com.argos.backend_services.service.AnimalExceptionService;
import com.argos.backend_services.service.JwtService;
import com.argos.backend_services.service.PropertyAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AnimalExceptionController.class, AnimalApplicationController.class})
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationFilter.class,
        AnimalExceptionService.class, AnimalApplicationService.class,
        WebConfig.class, CurrentUserArgumentResolver.class, FirebaseTokenArgumentResolver.class,
        CustomGlobalException.class})
@TestPropertySource(properties = {
        "security.jwt.secret=test-secret-minimo-32-caracteres-para-hs256",
        "security.jwt.expiration-ms=86400000"
})
class AnimalExceptionApplicationFlowTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROPERTY_ID = UUID.randomUUID();
    private static final UUID ORIGIN_LOT_ID = UUID.randomUUID();
    private static final UUID BATCH_ID = UUID.randomUUID();
    private static final UUID MEDICINE_ID = UUID.randomUUID();
    private static final String EAR_CODE = "BR-0001";
    private static final String REASON = "Doença respiratória";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnimalExceptionRepository animalExceptionRepository;
    @MockBean
    private ApplicationAnimalRepository applicationAnimalRepository;
    @MockBean
    private LotRepository lotRepository;
    @MockBean
    private PropertyAccessService propertyAccessService;
    @MockBean
    private DatabaseRoutineRepository databaseRoutineRepository;
    @MockBean
    private AnimalExceptionMapper animalExceptionMapper;
    @MockBean
    private MedicineBatchRepository medicineBatchRepository;
    @MockBean
    private UserRepository userRepository;

    private final AtomicReference<AnimalException> createdException = new AtomicReference<>();
    private Lot originLot;
    private MedicineBatch medicineBatch;

    @BeforeEach
    void setUp() {
        originLot = Lot.builder()
                .id(ORIGIN_LOT_ID)
                .lotCode("LOTE-01")
                .category("BOI")
                .active(true)
                .isExceptionLot(false)
                .herdId(Herd.builder()
                        .propertyId(Property.builder().id(PROPERTY_ID).build())
                        .build())
                .build();
        var exceptionLot = Lot.builder()
                .id(UUID.randomUUID())
                .lotCode("LOTE-01-EXC1")
                .category("BOI")
                .active(true)
                .isExceptionLot(true)
                .parentId(originLot)
                .build();
        medicineBatch = MedicineBatch.builder()
                .id(BATCH_ID)
                .medicineId(Medicine.builder().id(MEDICINE_ID).build())
                .build();
        createdException.set(null);

        when(lotRepository.findWithHerdAndPropertyById(ORIGIN_LOT_ID)).thenReturn(Optional.of(originLot));
        when(propertyAccessService.activePropertyIds(USER_ID)).thenReturn(List.of(PROPERTY_ID));
        when(animalExceptionRepository.findWithLotsById(any(UUID.class))).thenAnswer(invocation -> {
            var animalException = createdException.get();
            return animalException != null && animalException.getId().equals(invocation.getArgument(0))
                    ? Optional.of(animalException)
                    : Optional.empty();
        });
        when(animalExceptionRepository.findAllByFilters(eq(true), eq(ORIGIN_LOT_ID), eq(List.of(PROPERTY_ID)),
                any(Pageable.class))).thenAnswer(invocation -> {
            var animalException = createdException.get();
            return new PageImpl<>(animalException == null ? List.of() : List.of(animalException),
                    invocation.getArgument(3), animalException == null ? 0 : 1);
        });
        when(applicationAnimalRepository.findByAnimalExceptionIdIn(any())).thenReturn(List.of());
        when(animalExceptionMapper.toOut(any(AnimalException.class), isNull())).thenAnswer(invocation -> {
            var animalException = invocation.<AnimalException>getArgument(0);
            return AnimalExceptionOut.builder()
                    .id(animalException.getId())
                    .earTag(animalException.getEarTag())
                    .currentLotCode(animalException.getLotId().getLotCode())
                    .originLotCode(animalException.getOriginLotId().getLotCode())
                    .status(animalException.getStatus().name())
                    .build();
        });
        when(medicineBatchRepository.findById(BATCH_ID)).thenReturn(Optional.of(medicineBatch));
        when(databaseRoutineRepository.calculateSlaughterDate(eq(MEDICINE_ID), any(LocalDate.class)))
                .thenReturn(LocalDate.now().plusDays(10));

        doAnswer(invocation -> {
            var exception = AnimalException.builder()
                    .id(UUID.randomUUID())
                    .lotId(exceptionLot)
                    .originLotId(originLot)
                    .earTag(invocation.getArgument(1))
                    .exceptionReason(invocation.getArgument(2))
                    .status(AnimalException.StatusAnimal.ATIVO)
                    .build();
            createdException.set(exception);
            return null;
        }).when(databaseRoutineRepository).registerException(
                eq(ORIGIN_LOT_ID), eq(EAR_CODE), eq(REASON), any(BigDecimal.class));
    }

    @Test
    void shouldCreateExceptionLotListAnimalAndApplyMedicineThroughHttpRoutes() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken(
                new CurrentUserData(USER_ID, "tester@argos.com", "Test User", "USER", PROPERTY_ID),
                null, List.of());

        mockMvc.perform(post("/animal-exceptions")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originLotId":"%s","earTag":"%s","exceptionReason":"%s","currentWeight":420.5}
                                """.formatted(ORIGIN_LOT_ID, EAR_CODE, REASON)))
                .andExpect(status().isCreated());

        var created = createdException.get();
        org.junit.jupiter.api.Assertions.assertNotNull(created, "A exceção deve ser criada pela rotina do banco");
        org.junit.jupiter.api.Assertions.assertTrue(created.getLotId().isExceptionLot());
        org.junit.jupiter.api.Assertions.assertEquals(ORIGIN_LOT_ID, created.getLotId().getParentId().getId());
        verify(databaseRoutineRepository).registerException(ORIGIN_LOT_ID, EAR_CODE, REASON,
                new BigDecimal("420.5"));

        mockMvc.perform(get("/animal-exceptions")
                        .param("active", "true")
                        .param("originId", ORIGIN_LOT_ID.toString())
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].earTag").value(EAR_CODE))
                .andExpect(jsonPath("$.data[0].currentLotCode").value("LOTE-01-EXC1"))
                .andExpect(jsonPath("$.data[0].originLotCode").value("LOTE-01"));

        var path = "/animal-application/" + created.getId();
        mockMvc.perform(post(path)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"earCode":"%s","medicineBatchId":"%s","dose":5,"unit":"ml","route":"ORAL","goal":"Tratamento"}
                                """.formatted(EAR_CODE, BATCH_ID)))
                .andExpect(status().isCreated());

        verify(databaseRoutineRepository).registerAnimalApplication(
                created.getId(), BATCH_ID, USER_ID, new BigDecimal("5"), "ml", "ORAL", "Tratamento");
        verify(databaseRoutineRepository, times(0)).returnException(created.getId());

        mockMvc.perform(post(path)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"earCode":"BR-9999","medicineBatchId":"%s","dose":5,"unit":"ml","route":"ORAL"}
                                """.formatted(BATCH_ID)))
                .andExpect(status().isBadRequest());
        verify(databaseRoutineRepository, times(1)).registerAnimalApplication(
                created.getId(), BATCH_ID, USER_ID, new BigDecimal("5"), "ml", "ORAL", "Tratamento");
    }
}
