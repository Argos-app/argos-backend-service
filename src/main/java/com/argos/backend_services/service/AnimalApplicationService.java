package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.AnimalApplicationIn;
import com.argos.backend_services.entity.AnimalException;
import com.argos.backend_services.exception.AnimalExceptionNotFoundException;
import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.exception.MedicineNotFoundException;
import com.argos.backend_services.repository.AnimalExceptionRepository;
import com.argos.backend_services.repository.DatabaseRoutineRepository;
import com.argos.backend_services.repository.MedicineBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnimalApplicationService {

    private final AnimalExceptionRepository animalExceptionRepository;
    private final MedicineBatchRepository medicineBatchRepository;
    private final PropertyAccessService propertyAccessService;
    private final DatabaseRoutineRepository databaseRoutineRepository;

    public void register(UUID animalExceptionId, AnimalApplicationIn input, UUID userId)
            throws MedicineNotFoundException {
        var animalException = animalExceptionRepository.findWithLotsById(animalExceptionId)
                .orElseThrow(() -> new AnimalExceptionNotFoundException(animalExceptionId));
        var propertyId = animalException.getOriginLotId().getHerdId().getPropertyId().getId();
        propertyAccessService.requireActiveAccess(userId, propertyId);

        if (animalException.getStatus() != AnimalException.StatusAnimal.ATIVO) {
            throw new BadRequestException("Aplicações individuais só podem ser registradas para exceções ativas");
        }

        if (!Objects.equals(animalException.getEarTag(), input.earCode())) {
            throw new BadRequestException("O ear_Code informado não corresponde ao animal da URL");
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

}
