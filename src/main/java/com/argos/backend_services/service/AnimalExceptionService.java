package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.AnimalExceptionIn;
import com.argos.backend_services.dto.out.AnimalExceptionOut;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.entity.AnimalException;
import com.argos.backend_services.entity.ApplicationAnimal;
import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.mapper.AnimalExceptionMapper;
import com.argos.backend_services.repository.AnimalExceptionRepository;
import com.argos.backend_services.repository.ApplicationAnimalRepository;
import com.argos.backend_services.repository.DatabaseRoutineRepository;
import com.argos.backend_services.repository.LotRepository;
import com.argos.backend_services.repository.PropertyRepository;
import com.argos.backend_services.utils.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnimalExceptionService {

    private final AnimalExceptionRepository animalExceptionRepository;
    private final ApplicationAnimalRepository applicationAnimalRepository;
    private final LotRepository lotRepository;
    private final PropertyRepository propertyRepository;
    private final DatabaseRoutineRepository databaseRoutineRepository;
    private final AnimalExceptionMapper mapper;

    public void register(AnimalExceptionIn input, UUID userId) throws LotNotFoundException {
        var originLot = lotRepository.findWithHerdAndPropertyById(input.originLotId())
                .orElseThrow(() -> new LotNotFoundException("Lote de origem não encontrado: " + input.originLotId()));

        validateLotAccess(originLot.getHerdId().getPropertyId().getId(), propertyIdsByUser(userId));
        if (!originLot.isActive()) {
            throw new BadRequestException("Lote de origem não está ativo");
        }

        databaseRoutineRepository.registerException(
                originLot.getId(),
                input.earTag(),
                input.exceptionReason(),
                input.currentWeight());
    }

    @Transactional(readOnly = true)
    public PageOut<AnimalExceptionOut> list(Boolean active, UUID originId, UUID userId, int page, int size) {
        var pageable = PageableUtils.createPageRequest(page, size, Sort.by(Sort.Direction.DESC, "exceptionDate"));
        var exceptions = animalExceptionRepository.findAllByFilters(
                active,
                originId,
                propertyIdsByUser(userId),
                pageable);

        var animalApplications = exceptions.isEmpty()
                ? List.<ApplicationAnimal>of()
                : applicationAnimalRepository.findByAnimalExceptionIdIn(exceptions.getContent());

        var data = exceptions.getContent().stream()
                .map(animalException -> mapper.toOut(animalException,
                        mapper.lastApplication(animalException, animalApplications)))
                .toList();

        return new PageOut<>(
                data,
                exceptions.getNumber(),
                exceptions.getSize(),
                exceptions.getTotalElements(),
                exceptions.getTotalPages(),
                exceptions.isFirst(),
                exceptions.isLast());
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

    private void validateLotAccess(UUID lotPropertyId, List<UUID> userPropertyIds) {
        if (!userPropertyIds.contains(lotPropertyId)) {
            throw new BadRequestException("Lote de origem não pertence a uma fazenda do usuário");
        }
    }
}
