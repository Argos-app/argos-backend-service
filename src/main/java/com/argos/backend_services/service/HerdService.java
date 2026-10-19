package com.argos.backend_services.service;

import com.argos.backend_services.dto.HerdOut;
import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.repository.HerdRepository;
import com.argos.backend_services.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HerdService {

    private final HerdRepository herdRepository;
    private final PropertyRepository propertyRepository;

    public List<HerdOut> listHerd(UUID userId, UUID propertyId) {
        var property = propertyRepository.findByUserIdAndActiveTrue(userId).getFirst();
        var herd = herdRepository.findByPropertyId(property);
        return herd.stream()
                .map(herds -> HerdOut.builder()
                        .propertyId(herds.getPropertyId().getId())
                        .herdName(herds.getHerdName())
                        .breed(herds.getBreed())
                        .headCount(herds.getHeadCount())
                        .build())
                .toList();
    }
}
