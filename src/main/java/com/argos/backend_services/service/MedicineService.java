package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.MedicineBatchIn;
import com.argos.backend_services.dto.out.MedicineCreatedOut;
import com.argos.backend_services.dto.out.MedicineListItemOut;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.entity.Medicine;
import com.argos.backend_services.entity.MedicineBatch;
import com.argos.backend_services.entity.StockMovement;
import com.argos.backend_services.entity.Supplier;
import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.exception.NotFoundException;
import com.argos.backend_services.mapper.MedicineMapper;
import com.argos.backend_services.repository.DatabaseRoutineRepository;
import com.argos.backend_services.repository.MedicineBatchRepository;
import com.argos.backend_services.repository.MedicineRepository;
import com.argos.backend_services.repository.StockMovementRepository;
import com.argos.backend_services.repository.SupplierRepository;
import com.argos.backend_services.repository.UserRepository;
import com.argos.backend_services.repository.PropertyRepository;
import com.argos.backend_services.utils.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository medicineBatchRepository;
    private final SupplierRepository supplierRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final PropertyAccessService propertyAccessService;
    private final DatabaseRoutineRepository databaseRoutineRepository;
    private final MedicineMapper medicineMapper;
    private final Clock clock;

    @Transactional
    public MedicineCreatedOut registerBatch(MedicineBatchIn input, UUID userId) {
        var today = LocalDate.now(clock);
        validateDates(input, today);

        var medicine = medicineRepository.findByMapaRegistration(input.productRegistration().trim())
                .filter(medicineCandidate -> medicineCandidate.isActive())
                .orElseThrow(() -> new NotFoundException("Medicamento não encontrado para o registro MAPA informado"));

        if (medicineBatchRepository.existsByMedicineIdAndBatchNumber(
                medicine, input.batchNumber().trim())) {
            throw new BadRequestException("Já existe um lote com este número para o medicamento informado");
        }

        var user = userRepository.findById(userId)
                .filter(userCandidate -> Boolean.TRUE.equals(userCandidate.getActive()))
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado ou inativo"));

        var batch = medicineBatchRepository.save(MedicineBatch.builder()
                .medicineId(medicine)
                .batchNumber(input.batchNumber().trim())
                .manufactureDate(input.manufactureDate())
                .expiryDate(input.expiryDate())
                .deliveryDate(today)
                .initialQuantity(input.quantity())
                .status(MedicineBatch.Status.DISPONIVEL)
                .build());

        stockMovementRepository.save(StockMovement.builder()
                .medicineBatchId(batch)
                .userId(user)
                .type('E')
                .quantity(input.quantity())
                .reason("ENTRADA_LOTE")
                .build());

        return new MedicineCreatedOut(batch.getId());
    }

    @Transactional(readOnly = true)
    public PageOut<MedicineListItemOut> list(UUID therapeuticCategoryId, int page, int size, UUID userId) {
        var pageable = PageableUtils.createPageRequest(page, size,
                Sort.by(Sort.Order.asc("expiryDate"), Sort.Order.asc("medicineId.brandName"),
                        Sort.Order.asc("batchNumber")));

        var result = filterByTherapeuticCategory(
                therapeuticCategoryId, pageable);
        var data = result.getContent().stream()
                .map(batch -> medicineMapper.toListItemOut(
                        batch,
                        databaseRoutineRepository.balanceMedicineBatch(batch.getId())))
                .toList();

        return new PageOut<>(
                data,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast());
    }

    private void findOrCreateSupplier(MedicineBatchIn input) {
        var cnpj = input.supplierCnpj().trim();
        if (supplierRepository.findByCnpj(cnpj).isPresent()) {
            var supplier = supplierRepository.save(Supplier.builder()
                    .companyName(input.supplierName().trim())
                    .cnpj(cnpj)
                    .active(true)
                    .build());

            var medicine = medicineRepository.findByMapaRegistration(input.productRegistration());
            medicineRepository.save(Medicine
                    .builder()
                    .createdAt(medicine.get().getCreatedAt())
                    .needsPrescription(medicine.get().isNeedsPrescription())
                    .id(medicine.get().getId())
                    .supplierId(supplier)
                    .activeIngredient(medicine.get().getActiveIngredient())
                    .description(medicine.get().getDescription())
                    .dose(medicine.get().getDose())
                    .active(true)
                    .build());
        }
    }

    private void validateDates(MedicineBatchIn input, LocalDate today) {
        if (!input.expiryDate().isAfter(input.manufactureDate())) {
            throw new BadRequestException("A data de validade deve ser posterior à data de fabricação");
        }
        if (!input.expiryDate().isAfter(today)) {
            throw new BadRequestException("A data de validade deve ser futura");
        }
    }

    private Page<MedicineBatch> filterByTherapeuticCategory(
            UUID therapeuticCategoryId, Pageable pageable) {

        return therapeuticCategoryId == null
                ? medicineBatchRepository.findByStatus(MedicineBatch.Status.DISPONIVEL, pageable)
                : medicineBatchRepository.findByStatusAndMedicineId_TherapeuticCategoryId(
                MedicineBatch.Status.DISPONIVEL, therapeuticCategoryId, pageable);
    }
}
