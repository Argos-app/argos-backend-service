package com.argos.backend_services.service;

import com.argos.backend_services.repository.PropertyRepository;
import com.argos.backend_services.repository.UserPropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PropertyAccessService {

    private final UserPropertyRepository userPropertyRepository;
    private final PropertyRepository propertyRepository;

    @Transactional(readOnly = true)
    public void requireActiveAccess(UUID userId, UUID propertyId) {
        boolean isPropertyAdministrator = propertyRepository.existsByIdAndUser_IdAndActiveTrue(propertyId, userId);
        boolean hasActiveUserPropertyLink = userPropertyRepository
                .existsByUser_IdAndProperty_IdAndActiveTrue(userId, propertyId);
        if (!isPropertyAdministrator && !hasActiveUserPropertyLink) {
            throw new AccessDeniedException("Usuário não possui acesso à propriedade");
        }
    }

    @Transactional(readOnly = true)
    public List<UUID> activePropertyIds(UUID userId) {
        return Stream.concat(
                        propertyRepository.findByUserIdAndActiveTrue(userId).stream().map(property -> property.getId()),
                        userPropertyRepository.findByUser_IdAndActiveTrue(userId).stream()
                                .map(link -> link.getProperty().getId()))
                .distinct()
                .toList();
    }
}
