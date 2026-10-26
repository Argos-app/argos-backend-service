package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.LotIn;
import com.argos.backend_services.dto.out.LotOut;
import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.exception.PropertyNotFoundException;
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
    private final DatabaseRoutineRepository databaseRoutineRepository;
    private final LotMapper mapper;
    private final PropertyAccessService propertyAccessService;
    private final ReportHerdService reportHerdService;

    @Transactional(readOnly = true)
    public List<LotOut> listLot(UUID userId) {
        var propertyIds = propertyAccessService.activePropertyIds(userId);
        if (propertyIds.isEmpty()) {
            return List.of();
        }
        return lotRepository.findByActiveTrueAndHerdId_PropertyId_IdIn(propertyIds).stream()
                .map(mapper::toOut)
                .toList();
    }

    public void registerLot(LotIn input, UUID userId) throws LotNotFoundException, PropertyNotFoundException {
        var herd = herdRepository.findWithPropertyByHerdNameAndActiveTrue(input.herdName())
                .orElseThrow(() -> new LotNotFoundException("Herd not found: " + input.herdName()));

        propertyAccessService.requireActiveAccess(userId, herd.getPropertyId().getId());
        var lot = mapper.toEntity(input, herd);
        lotRepository.save(lot);
        reportHerdService.saveHerd(userId, herd.getPropertyId().getId());
    }

    public void deleteLot(UUID lotId, UUID userId) throws LotNotFoundException {
        var lot = lotRepository.findWithHerdAndPropertyById(lotId)
                .orElseThrow(() -> new LotNotFoundException("Lote não achado pelo id: " + lotId));

        propertyAccessService.requireActiveAccess(userId, lot.getHerdId().getPropertyId().getId());
        if (!Boolean.TRUE.equals(databaseRoutineRepository.lotIsReleaseForSlaughter(lotId, LocalDate.now()))) {
            throw new LotNotFoundException("Lote não disponível para abate");
        }
        lot.setActive(false);
        lotRepository.save(lot);
    }
}
