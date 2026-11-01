package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.LotIn;
import com.argos.backend_services.dto.out.LotOut;
import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.exception.NotFoundException;
import com.argos.backend_services.exception.PropertyNotFoundException;
import com.argos.backend_services.mapper.LotMapper;
import com.argos.backend_services.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.hibernate.exception.ConstraintViolationException;

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
    private final PropertyRepository propertyRepository;

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

    @Transactional(rollbackFor = Exception.class)
    public void registerLot(LotIn input, UUID userId, UUID propertyId) throws PropertyNotFoundException {
        var herd = input.herdId() == null
                ? createHerd(input, userId, propertyId)
                : findHerd(input.herdId(), userId);
        var lot = mapper.toEntity(input, herd);
        lotRepository.saveAndFlush(lot);
        reportHerdService.saveSingleHerd(userId, herd.getId());
    }

    private Herd findHerd(UUID herdId, UUID userId) {
        var herd = herdRepository.findWithPropertyByIdAndActiveTrue(herdId)
                .orElseThrow(() -> new NotFoundException("Rebanho não encontrado"));
        propertyAccessService.requireActiveAccess(userId, herd.getPropertyId().getId());
        return herd;
    }

    private Herd createHerd(LotIn input, UUID userId, UUID propertyId) {
        var property = resolveProperty(userId, propertyId);
        propertyAccessService.requireActiveAccess(userId, property.getId());
        if (herdRepository.existsByPropertyId_IdAndHerdName(property.getId(), input.herdName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe rebanho com esse nome na propriedade");
        }
        var herd = Herd.builder().propertyId(property).herdName(input.herdName()).breed(input.breed())
                .headCount(0).active(true).build();
        try {
            herdRepository.save(herd);
            return herd;
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && "uq_herd_nome".equals(violation.getConstraintName())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Já existe rebanho com esse nome na propriedade", exception);
                }
            }
            throw exception;
        }
    }

    private Property resolveProperty(UUID userId, UUID propertyId) {
        if (propertyId != null) {
            return propertyRepository.findById(propertyId).filter(property -> Boolean.TRUE.equals(property.getActive()))
                    .orElseThrow(() -> new NotFoundException("Propriedade não encontrada"));
        }
        var properties = propertyRepository.findAllById(propertyAccessService.activePropertyIds(userId)).stream()
                .filter(property -> Boolean.TRUE.equals(property.getActive())).toList();
        if (properties.isEmpty()) {
            throw new NotFoundException("Propriedade não encontrada");
        }
        if (properties.size() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "É necessário selecionar uma propriedade no contexto autenticado");
        }
        return properties.getFirst();
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
