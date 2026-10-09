package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.AnimalApplicationIn;
import com.argos.backend_services.entity.AnimalException;
import com.argos.backend_services.exception.AnimalExceptionNotFoundException;
import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.exception.MedicineNotFoundException;
import com.argos.backend_services.repository.AnimalExceptionRepository;
import com.argos.backend_services.repository.DatabaseRoutineRepository;
import com.argos.backend_services.repository.MedicineBatchRepository;
import com.argos.backend_services.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnimalApplicationService {

    private final AnimalExceptionRepository animalExceptionRepository;
    private final MedicineBatchRepository medicineBatchRepository;
    private final PropertyRepository propertyRepository;
    private final DatabaseRoutineRepository databaseRoutineRepository;

    public void register(UUID animalExceptionId, AnimalApplicationIn input, UUID userId)
            throws MedicineNotFoundException {
        var animalException = animalExceptionRepository.findWithLotsById(animalExceptionId)
                .orElseThrow(() -> new AnimalExceptionNotFoundException(animalExceptionId));
        validateAnimalAccess(animalException, propertyIdsByUser(userId));

        if (animalException.getStatus() != AnimalException.StatusAnimal.ATIVO) {
            throw new BadRequestException("Aplicações individuais só podem ser registradas para exceções ativas");
        }

        var medicineBatch = medicineBatchRepository.findById(input.medicineBatchId())
                .orElseThrow(() -> new MedicineNotFoundException("Lote de medicamento não encontrado"));

        databaseRoutineRepository.registerAnimalApplication(
                animalExceptionId,
                medicineBatch.getId(),
                userId,
                input.dose(),
                input.unit(),
                input.route(),
                input.goal());

        returnToOriginLotWhenEligible(animalException, medicineBatch.getMedicineId().getId());
    }

    private void returnToOriginLotWhenEligible(AnimalException animalException, UUID medicineId) {
        var withdrawalEnd = databaseRoutineRepository.calculateSlaughterDate(medicineId, LocalDate.now());
        var originLotWasNotSlaughtered = animalException.getOriginLotId().isActive();

        if (withdrawalEnd != null && !withdrawalEnd.isAfter(LocalDate.now()) && originLotWasNotSlaughtered) {
            databaseRoutineRepository.returnException(animalException.getId());
        }
    }

    private List<UUID> propertyIdsByUser(UUID userId) {
        var propertyIds = propertyRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(property -> property.getId())
                .toList();
        if (propertyIds.isEmpty()) {
            throw new BadRequestException("Usuário não possui fazenda ativa");
        }
        return propertyIds;
    }

    private void validateAnimalAccess(AnimalException animalException, List<UUID> userPropertyIds) {
        if (!userPropertyIds.contains(animalException.getOriginLotId().getHerdId().getPropertyId().getId())) {
            throw new BadRequestException("Exceção animal não pertence a uma fazenda do usuário");
        }
    }
}
