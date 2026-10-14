package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.ApplicationIn;
import com.argos.backend_services.dto.out.ApplicationOut;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.exception.MedicineNotFoundException;
import com.argos.backend_services.mapper.ApplicationMapper;
import com.argos.backend_services.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final LotRepository lotRepository;
    private final ApplicationRepository applicationRepository;
    private final MedicineBatchRepository medicineBatchRepository;
    private final MedicineRepository medicineRepository;
    private final DatabaseRoutineRepository databaseRoutineRepository;
    private final ApplicationAnimalRepository applicationAnimalRepository;
    private final ApplicationMapper mapper;
    private final PropertyAccessService propertyAccessService;

    public void registerApplication(ApplicationIn applicationIn, UUID userId) throws LotNotFoundException, MedicineNotFoundException {

        var lot = lotRepository.findWithHerdAndPropertyByLotCode(applicationIn.lotName())
                .orElseThrow(() -> new LotNotFoundException("Não foi possível achar o lot pelo nome"));

        var propertyId = lot.getHerdId().getPropertyId().getId();
        propertyAccessService.requireActiveAccess(userId, propertyId);

        var medicine = medicineRepository.findByBrandName(applicationIn.medicineName())
                .orElseThrow(() -> new MedicineNotFoundException("Could not found medicine by name"));

        var medicineBatch = medicineBatchRepository.findByMedicineIdAndProperty_Id(medicine, propertyId)
                .orElseThrow(() -> new MedicineNotFoundException("Could not found medicine batch by medicine id"));

        databaseRoutineRepository.registerApplication
                (lot.getId(),
                medicineBatch.getId(),
                userId,
                applicationIn.dose(),
                applicationIn.unit(),
                applicationIn.route(),
                applicationIn.goal());
    }

    public List<ApplicationOut> listApplication(UUID userId) {
        var propertyIds = propertyAccessService.activePropertyIds(userId);
        if (propertyIds.isEmpty()) {
            return List.of();
        }
        var applications = applicationRepository.findByLotId_HerdId_PropertyId_IdIn(propertyIds);
        var result = mapper.listToOut(applications);
        if (applicationAnimalRepository != null) {
            applications.forEach(application -> applicationAnimalRepository.findByApplicationId(application)
                    .stream()
                    .map(applicationAnimal -> mapper.toOut(applicationAnimal, application))
                    .forEach(result::add));
        }
        return result;
    }
}
