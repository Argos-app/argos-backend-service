package com.argos.backend_services.service;

import com.argos.backend_services.dto.out.UserListItemOut;
import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.repository.UserAiUsageRepository;
import com.argos.backend_services.repository.UserAiUsageRepository.Metrics;
import com.argos.backend_services.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAiUsageServiceTest {
    @Mock UserRepository users;
    @Mock UserAiUsageRepository usage;
    @InjectMocks UserAiUsageService service;
    private final UUID administratorId = UUID.randomUUID();

    @Test
    void shouldMergeOnlyRequestedUsersWithTheirMetricsInDatabaseOrder() {
        var first = user("A User", null);
        var second = user("B User", "https://example.com/photo.png");
        var pageable = PageRequest.of(1, 2);
        when(users.findLinkedByAdministrator(administratorId, pageable))
                .thenReturn(new PageImpl<>(List.of(first, second), pageable, 7));
        var values = new Metrics(Long.MAX_VALUE, 34500, 10, 2, 9, 3, new BigDecimal("0.0123456789123456789"));
        var zero = new Metrics(0, 0, 0, 0, 0, 0, BigDecimal.ZERO);
        when(usage.findByUserIds(List.of(first.userId(), second.userId())))
                .thenReturn(Map.of(second.userId(), zero, first.userId(), values));

        var result = service.list(1, 2, administratorId);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(7);
        assertThat(result.totalPages()).isEqualTo(4);
        assertThat(result.first()).isFalse();
        assertThat(result.last()).isFalse();
        assertThat(result.data()).extracting(item -> item.userId()).containsExactly(first.userId(), second.userId());
        var output = result.data().getFirst();
        assertThat(output.name()).isEqualTo(first.name());
        assertThat(output.email()).isEqualTo(first.email());
        assertThat(output.photoUrl()).isEmpty();
        assertThat(output.calls()).isEqualTo(Long.MAX_VALUE);
        assertThat(output.tokens()).isEqualTo(34500);
        assertThat(output.modelCalls()).isEqualTo(10);
        assertThat(output.unknownTokenCalls()).isEqualTo(2);
        assertThat(output.knownCostCalls()).isEqualTo(9);
        assertThat(output.unknownCostCalls()).isEqualTo(3);
        assertThat(output.costUsd()).isEqualTo(values.costUsd());
        assertThat(result.data().get(1).photoUrl()).isEqualTo(second.photoUrl());
        assertThat(result.data().get(1).calls()).isZero();
        verifyNoMoreInteractions(users, usage);
    }

    @ParameterizedTest
    @CsvSource({"0,0", "2,3"})
    void shouldReturnEmptyPagesWithoutReadingRedis(int page, long total) {
        var pageable = PageRequest.of(page, 10);
        when(users.findLinkedByAdministrator(administratorId, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, total));

        var result = service.list(page, 10, administratorId);

        assertThat(result.data()).isEmpty();
        assertThat(result.page()).isEqualTo(page);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(total);
        assertThat(result.totalPages()).isEqualTo(total == 0 ? 0 : 1);
        assertThat(result.first()).isEqualTo(page == 0);
        assertThat(result.last()).isTrue();
        verifyNoInteractions(usage);
    }

    @ParameterizedTest
    @CsvSource({"-1,10", "0,0", "0,-1", "0,101"})
    void shouldRejectInvalidPaginationBeforeReadingDependencies(int page, int size) {
        assertThatThrownBy(() -> service.list(page, size, administratorId)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(users, usage);
    }

    @ParameterizedTest
    @CsvSource({"0,1", "0,100"})
    void shouldAcceptPaginationLimits(int page, int size) {
        when(users.findLinkedByAdministrator(administratorId, PageRequest.of(page, size)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(page, size), 0));
        assertThat(service.list(page, size, administratorId).size()).isEqualTo(size);
    }

    private UserListItemOut user(String name, String photoUrl) {
        return new UserListItemOut(UUID.randomUUID(), photoUrl, name, "12345678901", "user@example.com",
                false, UUID.randomUUID());
    }
}
