package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.LotIn;
import com.argos.backend_services.dto.out.LotOut;
import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.mapper.LotMapper;
import com.argos.backend_services.repository.*;
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
    private final DatabaseRoutineRepository databaseRoutineRepository;
    private LotMapper mapper;

    @Transactional(readOnly = true)
    public List<LotOut> listLot(UUID userId) {
        return lotRepository.findAll().stream()
                .filter(Lot::isActive)
                .map(mapper::toOut)
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
        if (!databaseRoutineRepository.loteAptoAbate(lotId, LocalDate.now())) {
            throw new LotNotFoundException("Lot not release for slaughter");
        }
        lot.setActive(false);
        lotRepository.save(lot);
    }
}
