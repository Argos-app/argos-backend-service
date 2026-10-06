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
    private final DatabaseRoutineRepository databaseRoutineRepository;
    private final LotMapper mapper;

    @Transactional(readOnly = true)
    public List<LotOut> listLot(UUID userId) {
        return lotRepository.findAll().stream()
                .filter(Lot::isActive)
                .map(mapper::toOut)
                .toList();
    }

    public void registerLot(LotIn input) throws LotNotFoundException {
        var lot = mapper.toEntity(input);
        lotRepository.save(lot);
    }

    public void deleteLot(UUID lotId, UUID userId) throws LotNotFoundException {
        var lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new LotNotFoundException("Lote não achado pelo id: " + lotId));
        if (!Boolean.TRUE.equals(databaseRoutineRepository.lotIsReleaseForSlaughter(lotId, LocalDate.now()))) {
            throw new LotNotFoundException("Lote não disponível para abate");
        }
        lot.setActive(false);
        lotRepository.save(lot);
    }
}
