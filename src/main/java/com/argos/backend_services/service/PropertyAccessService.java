package com.argos.backend_services.service;

import com.argos.backend_services.repository.UserPropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropertyAccessService {

    private final UserPropertyRepository userPropertyRepository;

    @Transactional(readOnly = true)
    public void requireActiveAccess(UUID userId, UUID propertyId) {
        if (!userPropertyRepository.existsByUser_IdAndProperty_IdAndActiveTrue(userId, propertyId)) {
            throw new AccessDeniedException("Usuário não possui acesso à propriedade");
        }
    }

    @Transactional(readOnly = true)
    public List<UUID> activePropertyIds(UUID userId) {
        return userPropertyRepository.findByUser_IdAndActiveTrue(userId).stream()
                .map(link -> link.getProperty().getId())
                .toList();
    }
}
