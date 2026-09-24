package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.LotIn;
import com.argos.backend_services.dto.out.LotOut;
import com.argos.backend_services.entity.AnimalException;
import com.argos.backend_services.entity.ApplicationAnimal;
import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.mapper.LotMapper;
import com.argos.backend_services.repository.AnimalExceptionRepository;
import com.argos.backend_services.repository.ApplicationAnimalRepository;
import com.argos.backend_services.repository.HerdRepository;
import com.argos.backend_services.repository.LotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LotService {

    private final LotRepository lotRepository;
    private final HerdRepository herdRepository;
    private final AnimalExceptionRepository animalExceptionRepository;
    private final ApplicationAnimalRepository applicationAnimalRepository;
    private final LotMapper mapper;

    @Transactional(readOnly = true)
    public List<LotOut> listLot(UUID userId) {
        return lotRepository.findAll().stream()
                .filter(Lot::isActive)
                .map(this::toOut)
                .toList();
    }

    public void registerLot(LotIn input) throws LotNotFoundException {
        var herd = herdRepository.findByHerdNameAndActiveTrue(input.herdName())
                .orElseThrow(() -> new LotNotFoundException("Herd not found: " + input.herdName()));
        var lot = mapper.toEntity(input);
        lot.setHerdId(herd);
        lotRepository.save(lot);
    }

    public void deleteLot(UUID lotId, UUID userId) throws LotNotFoundException {
        var lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new LotNotFoundException("Lot not found: " + lotId));
        lot.setActive(false);
        lotRepository.save(lot);
    }

    private LotOut toOut(Lot lot) {
        var output = mapper.toOut(lot);
        if (!lot.isExceptionLot()) return output;
        var animals = animalExceptionRepository.findByLotId(lot);
        var details = animals.stream().map(animal -> {
            var applications = applicationAnimalRepository.findByAnimalExceptionId(animal);
            ApplicationAnimal latest = applications.stream().reduce((first, second) -> second).orElse(null);
            return LotOut.AnimalExceptionOut.builder()
                    .earCode(animal.getEarTag())
                    .medicine(latest == null ? null : latest.getApplicationId().getMedicineBatchId().getMedicineId().getBrandName())
                    .releaseSlaughterDate(formatDate(latest == null ? animal.getSlaughterReleaseDate() : latest.getMeatWithdrawalDate()))
                    .build();
        }).toList();
        output.setAnimalExceptionOutList(details);
        return output;
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : date.toString();
    }
}