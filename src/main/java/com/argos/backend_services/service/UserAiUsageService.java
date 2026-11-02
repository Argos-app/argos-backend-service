package com.argos.backend_services.service;

import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.dto.out.UserAiUsageOut;
import com.argos.backend_services.dto.out.UserListItemOut;
import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.repository.UserAiUsageRepository;
import com.argos.backend_services.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAiUsageService {
    private final UserRepository users;
    private final UserAiUsageRepository usage;

    public PageOut<UserAiUsageOut> list(int page, int size, UUID administratorId) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Parâmetros de paginação inválidos");
        }

        Page<UserListItemOut> result = users.findLinkedByAdministrator(administratorId, PageRequest.of(page, size));
        if (result.isEmpty()) {
            return new PageOut<>(List.of(), result.getNumber(), result.getSize(), result.getTotalElements(),
                    result.getTotalPages(), result.isFirst(), result.isLast());
        }

        var metrics = usage.findByUserIds(result.getContent().stream().map(UserListItemOut::userId).toList());

        var data = result.map(user -> {
            var values = metrics.get(user.userId());
            return new UserAiUsageOut(user.userId(), user.name(), user.email(),
                    user.photoUrl() == null ? "" : user.photoUrl(), values.calls(), values.tokens(), values.modelCalls(),
                    values.unknownTokenCalls(), values.knownCostCalls(), values.unknownCostCalls(), values.costUsd());
        });

        return new PageOut<>(data.getContent(), data.getNumber(), data.getSize(), data.getTotalElements(),
                data.getTotalPages(), data.isFirst(), data.isLast());
    }
}
