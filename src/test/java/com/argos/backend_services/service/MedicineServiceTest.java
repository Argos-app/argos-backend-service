package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.MedicineBatchIn;
import com.argos.backend_services.dto.out.MedicineCatalogItemOut;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Medicine;
import com.argos.backend_services.entity.MedicineBatch;
import com.argos.backend_services.entity.Supplier;
import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.mapper.MedicineMapper;
import com.argos.backend_services.repository.DatabaseRoutineRepository;
import com.argos.backend_services.repository.MedicineBatchRepository;
import com.argos.backend_services.repository.MedicineRepository;
import com.argos.backend_services.repository.StockMovementRepository;
import com.argos.backend_services.repository.SupplierRepository;
import com.argos.backend_services.repository.UserRepository;
import com.argos.backend_services.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MedicineServiceTest {

    private MedicineRepository medicineRepository;
    private MedicineBatchRepository medicineBatchRepository;
    private SupplierRepository supplierRepository;
    private StockMovementRepository stockMovementRepository;
    private UserRepository userRepository;
    private DatabaseRoutineRepository databaseRoutineRepository;
    private MedicineMapper medicineMapper;
    private MedicineService service;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-11T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        medicineRepository = mock(MedicineRepository.class);
        medicineBatchRepository = mock(MedicineBatchRepository.class);
        supplierRepository = mock(SupplierRepository.class);
        stockMovementRepository = mock(StockMovementRepository.class);
        userRepository = mock(UserRepository.class);
        databaseRoutineRepository = mock(DatabaseRoutineRepository.class);
        medicineMapper = mock(MedicineMapper.class);
        service = new MedicineService(medicineRepository, medicineBatchRepository, supplierRepository,
                stockMovementRepository, userRepository, databaseRoutineRepository, medicineMapper, clock);
    }

    @Test
    void shouldCreateSupplierBatchAndInitialStockEntry() {
        var medicine = Medicine.builder().id(UUID.randomUUID()).active(true).needsPrescription(false).build();
        var user = AppUser.builder().id(UUID.randomUUID()).active(true).build();
        var supplier = Supplier.builder().id(UUID.randomUUID()).companyName("Fornecedor Teste").cnpj("12345678000199").active(true).build();
        var batchId = UUID.randomUUID();
        var input = validInput();

        when(medicineRepository.findByMapaRegistration(input.productRegistration())).thenReturn(Optional.of(medicine));
        when(medicineBatchRepository.existsByMedicineIdAndBatchNumber(
                medicine, input.batchNumber())).thenReturn(false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(supplierRepository.findByCnpj(input.supplierCnpj())).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);
        when(medicineBatchRepository.save(any(MedicineBatch.class))).thenAnswer(invocation -> {
            var batch = invocation.getArgument(0, MedicineBatch.class);
            batch.setId(batchId);
            return batch;
        });

        var result = service.registerBatch(input, user.getId());

        assertThat(result.medicineBatchId()).isEqualTo(batchId);
        var batchCaptor = ArgumentCaptor.forClass(MedicineBatch.class);
        verify(medicineBatchRepository).save(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getMedicineId()).isEqualTo(medicine);
        assertThat(batchCaptor.getValue().getDeliveryDate()).isEqualTo(LocalDate.of(2026, 10, 11));
        verify(stockMovementRepository).save(argThat(movement ->
                movement.getMedicineBatchId().getId().equals(batchId)
                        && movement.getUserId().equals(user)
                        && movement.getType() == 'E'
                        && movement.getQuantity().compareTo(input.quantity()) == 0
                        && "ENTRADA_LOTE".equals(movement.getReason())));
    }

    @Test
    void shouldRejectNonFutureExpiryDateBeforeAccessingRepositories() {
        var input = new MedicineBatchIn("12345", "LOTE-1", LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 10, 11), new BigDecimal("5.000"), "Fornecedor", "12345678000199");

        assertThatThrownBy(() -> service.registerBatch(input, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("validade deve ser futura");

        verifyNoInteractions(medicineRepository, medicineBatchRepository, supplierRepository,
                stockMovementRepository, userRepository, databaseRoutineRepository, medicineMapper);
    }

    @Test
    void shouldNotCreateSupplierWhenCnpjAlreadyExists() {
        var medicine = Medicine.builder().id(UUID.randomUUID()).active(true).needsPrescription(false).build();
        var user = AppUser.builder().id(UUID.randomUUID()).active(true).build();
        var supplier = Supplier.builder().id(UUID.randomUUID()).active(true).build();
        var input = validInput();

        when(medicineRepository.findByMapaRegistration(input.productRegistration())).thenReturn(Optional.of(medicine));
        when(medicineBatchRepository.existsByMedicineIdAndBatchNumber(
                medicine, input.batchNumber())).thenReturn(false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(supplierRepository.findByCnpj(input.supplierCnpj())).thenReturn(Optional.of(supplier));
        when(medicineBatchRepository.save(any(MedicineBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.registerBatch(input, user.getId());

        verify(supplierRepository, never()).save(any());
        verify(stockMovementRepository).save(any());
    }

    @Test
    void shouldListActiveCatalogItemsInStableOrderAndPreservePageMetadata() {
        var medicineId = UUID.randomUUID();
        var medicine = Medicine.builder().id(medicineId).brandName("Vacina B").build();
        var item = new MedicineCatalogItemOut(medicineId, "Vacina B", "MAPA-B");
        var pageRequest = org.springframework.data.domain.PageRequest.of(1, 1);
        when(medicineRepository.findByActiveTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(medicine), pageRequest, 2));
        when(medicineMapper.toCatalogItemOut(medicine)).thenReturn(item);

        PageOut<MedicineCatalogItemOut> result = service.listCatalog(1, 1);

        assertThat(result.data()).containsExactly(item);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.first()).isFalse();
        assertThat(result.last()).isTrue();
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(medicineRepository).findByActiveTrue(pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(
                Sort.by(Sort.Order.asc("brandName"), Sort.Order.asc("id")));
    }

    @Test
    void shouldRejectInvalidCatalogPaginationBeforeReadingRepository() {
        assertThatThrownBy(() -> service.listCatalog(-1, 20)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.listCatalog(0, 101)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(medicineRepository, medicineMapper);
    }

    private MedicineBatchIn validInput() {
        return new MedicineBatchIn("12345", "LOTE-001", LocalDate.of(2026, 1, 10),
                LocalDate.of(2027, 1, 10), new BigDecimal("500.000"),
                "Fornecedor Teste", "12345678000199");
    }
}
