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
import org.mapstruct.factory.Mappers;
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
    private PropertyAccessService propertyAccessService;
    private PropertyRepository propertyRepository;
    private final UUID propertyId = UUID.randomUUID();
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
        propertyAccessService = mock(PropertyAccessService.class);
        propertyRepository = mock(PropertyRepository.class);
        var property = com.argos.backend_services.entity.Property.builder().id(propertyId).active(true).build();
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(propertyRepository.getReferenceById(propertyId)).thenReturn(property);
        service = new MedicineService(medicineRepository, medicineBatchRepository, supplierRepository,
                stockMovementRepository, userRepository, databaseRoutineRepository, medicineMapper, clock, propertyAccessService, propertyRepository);
    }

    @Test
    void shouldCreateSupplierBatchAndInitialStockEntry() {
        var medicine = Medicine.builder().id(UUID.randomUUID()).active(true).needsPrescription(false).build();
        var user = AppUser.builder().id(UUID.randomUUID()).active(true).build();
        var supplier = Supplier.builder().id(UUID.randomUUID()).companyName("Fornecedor Teste").cnpj("12345678000199").active(true).build();
        var batchId = UUID.randomUUID();
        var input = validInput();

        when(medicineRepository.findByMapaRegistration(input.productRegistration())).thenReturn(Optional.of(medicine));
        when(medicineBatchRepository.existsByMedicineIdAndBatchNumberAndPropertyId_Id(
                medicine, input.batchNumber(), propertyId)).thenReturn(false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(supplierRepository.findByCnpj(input.supplierCnpj())).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);
        when(medicineBatchRepository.save(any(MedicineBatch.class))).thenAnswer(invocation -> {
            var batch = invocation.getArgument(0, MedicineBatch.class);
            batch.setId(batchId);
            return batch;
        });

        var result = service.registerBatch(input, user.getId(), propertyId);

        assertThat(result.medicineBatchId()).isEqualTo(batchId);
        assertThat(medicine.getSupplierId()).isEqualTo(supplier);
        var batchCaptor = ArgumentCaptor.forClass(MedicineBatch.class);
        verify(medicineBatchRepository).save(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getMedicineId()).isEqualTo(medicine);
        assertThat(batchCaptor.getValue().getPropertyId().getId()).isEqualTo(propertyId);
        assertThat(batchCaptor.getValue().getDeliveryDate()).isEqualTo(LocalDate.of(2026, 10, 11));
        verify(stockMovementRepository).save(argThat(movement ->
                movement.getMedicineBatchId().getId().equals(batchId)
                        && movement.getUserId().equals(user)
                        && movement.getType() == 'E'
                        && movement.getQuantity().compareTo(input.quantity()) == 0
                        && "ENTRADA_LOTE".equals(movement.getReason())));
    }

    @Test
    void registerBatchAssociatesSupplierWithMedicine() {
        var medicine = Medicine.builder().id(UUID.randomUUID()).active(true).needsPrescription(false).build();
        var user = AppUser.builder().id(UUID.randomUUID()).active(true).build();
        var supplier = Supplier.builder().id(UUID.randomUUID()).companyName("Novo Fornecedor")
                .cnpj("12345678000199").active(true).build();
        var input = new MedicineBatchIn("12345", "LOTE-002", LocalDate.of(2026, 1, 10),
                LocalDate.of(2027, 1, 10), new BigDecimal("20.000"), "Novo Fornecedor", "12345678000199");

        when(medicineRepository.findByMapaRegistration(input.productRegistration())).thenReturn(Optional.of(medicine));
        when(medicineBatchRepository.existsByMedicineIdAndBatchNumberAndPropertyId_Id(medicine, input.batchNumber(), propertyId)).thenReturn(false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(supplierRepository.findByCnpj(input.supplierCnpj())).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);
        when(medicineBatchRepository.save(any(MedicineBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.registerBatch(input, user.getId(), propertyId);

        var supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(supplierCaptor.capture());
        assertThat(supplierCaptor.getValue().getCompanyName()).isEqualTo("Novo Fornecedor");
        assertThat(supplierCaptor.getValue().getCnpj()).isEqualTo("12345678000199");
        verify(medicineRepository).save(medicine);
        assertThat(medicine.getSupplierId()).isEqualTo(supplier);
    }

    @Test
    void registerBatchReusesExistingSupplierAndAssociatesItWithMedicine() {
        var originalSupplier = Supplier.builder().id(UUID.randomUUID()).companyName("Fornecedor do catálogo")
                .cnpj("99999999000199").active(true).build();
        var existingSupplier = Supplier.builder().id(UUID.randomUUID()).companyName("Fornecedor do lote")
                .cnpj("12345678000199").active(true).build();
        var medicine = Medicine.builder().id(UUID.randomUUID()).supplierId(originalSupplier)
                .active(true).needsPrescription(false).build();
        var user = AppUser.builder().id(UUID.randomUUID()).active(true).build();
        var input = validInput();

        when(medicineRepository.findByMapaRegistration(input.productRegistration())).thenReturn(Optional.of(medicine));
        when(medicineBatchRepository.existsByMedicineIdAndBatchNumberAndPropertyId_Id(medicine, input.batchNumber(), propertyId)).thenReturn(false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(supplierRepository.findByCnpj(input.supplierCnpj())).thenReturn(Optional.of(existingSupplier));
        when(medicineBatchRepository.save(any(MedicineBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.registerBatch(input, user.getId(), propertyId);

        verify(supplierRepository, never()).save(any());
        verify(medicineRepository).save(medicine);
        assertThat(medicine.getSupplierId()).isEqualTo(existingSupplier);
    }

    @Test
    void registerBatchSupplierIsReturnedByStockListFromMedicine() {
        var catalogSupplier = Supplier.builder().id(UUID.randomUUID()).companyName("Fornecedor do catálogo")
                .cnpj("99999999000199").active(true).build();
        var medicine = Medicine.builder().id(UUID.randomUUID()).supplierId(catalogSupplier)
                .brandName("Ivermectina").activeIngredient("Ivermectina").unit("ml")
                .active(true).needsPrescription(false).build();
        var user = AppUser.builder().id(UUID.randomUUID()).active(true).build();
        var input = validInput();
        var batchId = UUID.randomUUID();
        var savedBatch = new MedicineBatch[1];
        var medicineSupplier = Supplier.builder().id(UUID.randomUUID()).companyName(input.supplierName())
                .cnpj(input.supplierCnpj()).active(true).build();

        medicineMapper = Mappers.getMapper(MedicineMapper.class);
        service = new MedicineService(medicineRepository, medicineBatchRepository, supplierRepository,
                stockMovementRepository, userRepository, databaseRoutineRepository, medicineMapper, clock, propertyAccessService, propertyRepository);
        when(medicineRepository.findByMapaRegistration(input.productRegistration())).thenReturn(Optional.of(medicine));
        when(medicineBatchRepository.existsByMedicineIdAndBatchNumberAndPropertyId_Id(medicine, input.batchNumber(), propertyId)).thenReturn(false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(supplierRepository.findByCnpj(input.supplierCnpj())).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenReturn(medicineSupplier);
        when(medicineBatchRepository.save(any(MedicineBatch.class))).thenAnswer(invocation -> {
            var batch = invocation.getArgument(0, MedicineBatch.class);
            batch.setId(batchId);
            savedBatch[0] = batch;
            return batch;
        });
        when(databaseRoutineRepository.balanceMedicineBatch(batchId)).thenReturn(new BigDecimal("450.000"));

        service.registerBatch(input, user.getId(), propertyId);
        when(medicineBatchRepository.findAvailableInProperty(eq(propertyId), eq(MedicineBatch.Status.DISPONIVEL), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(savedBatch[0])));

        var listed = service.list(null, 0, 20, user.getId(), propertyId).data().getFirst();

        assertThat(listed.medicineBatchId()).isEqualTo(batchId);
        assertThat(listed.supplier()).isEqualTo(input.supplierName());
        assertThat(listed.doseUnit()).isEqualTo("ml");
        assertThat(listed.quantity()).isEqualByComparingTo("450.000");
        assertThat(medicine.getSupplierId()).isEqualTo(medicineSupplier);
    }

    @Test
    void shouldRejectNonFutureExpiryDateBeforeAccessingRepositories() {
        var input = new MedicineBatchIn("12345", "LOTE-1", LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 10, 11), new BigDecimal("5.000"), "Fornecedor", "12345678000199");

        assertThatThrownBy(() -> service.registerBatch(input, UUID.randomUUID(), propertyId))
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
        when(medicineBatchRepository.existsByMedicineIdAndBatchNumberAndPropertyId_Id(
                medicine, input.batchNumber(), propertyId)).thenReturn(false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(supplierRepository.findByCnpj(input.supplierCnpj())).thenReturn(Optional.of(supplier));
        when(medicineBatchRepository.save(any(MedicineBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.registerBatch(input, user.getId(), propertyId);

        verify(supplierRepository, never()).save(any());
        verify(medicineRepository).save(medicine);
        assertThat(medicine.getSupplierId()).isEqualTo(supplier);
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

    @Test
    void listRestrictsQueryToSelectedPropertyAndCategory() {
        var userId = UUID.randomUUID();
        var categoryId = UUID.randomUUID();
        when(medicineBatchRepository.findAvailableInProperty(
                eq(propertyId), eq(MedicineBatch.Status.DISPONIVEL), eq(categoryId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = service.list(categoryId, 0, 20, userId, propertyId);

        assertThat(result.data()).isEmpty();
        verify(propertyAccessService).requireActiveAccess(userId, propertyId);
        verify(medicineBatchRepository).findAvailableInProperty(
                eq(propertyId), eq(MedicineBatch.Status.DISPONIVEL), eq(categoryId), any(Pageable.class));
        verify(databaseRoutineRepository, never()).balanceMedicineBatch(any());
    }

    @Test
    void listRejectsUnassociatedPropertyBeforeQuery() {
        var userId = UUID.randomUUID();
        doThrow(new org.springframework.security.access.AccessDeniedException("Sem acesso"))
                .when(propertyAccessService).requireActiveAccess(userId, propertyId);

        assertThatThrownBy(() -> service.list(null, 0, 20, userId, propertyId))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verifyNoInteractions(medicineBatchRepository);
    }

    private MedicineBatchIn validInput() {
        return new MedicineBatchIn("12345", "LOTE-001", LocalDate.of(2026, 1, 10),
                LocalDate.of(2027, 1, 10), new BigDecimal("500.000"),
                "Fornecedor Teste", "12345678000199");
    }
}
