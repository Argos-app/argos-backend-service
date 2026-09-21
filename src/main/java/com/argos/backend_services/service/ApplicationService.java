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
    private final ApplicationMapper mapper;

    public void registerApplication(ApplicationIn applicationIn, UUID userId) throws LotNotFoundException, MedicineNotFoundException {

        var lot = lotRepository.findByLotCode(applicationIn.lotName())
                .orElseThrow(() -> new LotNotFoundException("Could not foundo by lote"));

        var medicine = medicineRepository.findByBrandName(applicationIn.medicineName())
                .orElseThrow(() -> new MedicineNotFoundException("Could not found medicine by name"));

        var medicineBatch = medicineBatchRepository.findByMedicineId(medicine)
                .orElseThrow(() -> new MedicineNotFoundException("Could not found medicine batch by medicine id"));

        databaseRoutineRepository.registrarAplicacao
                (lot.getId(),
                medicineBatch.getId(),
                userId,
                applicationIn.dose(),
                applicationIn.unit(),
                applicationIn.route(),
                applicationIn.goal());
    }

    public List<ApplicationOut> listApplication() {
        return mapper.listToOut(applicationRepository.findAll());
    }
}
