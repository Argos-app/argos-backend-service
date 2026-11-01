package com.argos.backend_services.repository;

import com.argos.backend_services.exception.ArgosInternalServerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisHashCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAiUsageRepositoryTest {
    @Mock StringRedisTemplate redis;
    @Mock RedisConnection connection;
    @Mock RedisHashCommands hashes;
    @InjectMocks UserAiUsageRepository repository;

    @Test
    void shouldReadAllFieldsInOnePipelineAndKeepUserAssociation() {
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        pipeline(List.of(List.of("9223372036854775807", "34500", "10", "2", "9", "3", "0.0123456789123456789"),
                Arrays.asList(null, "25", null, null, null, null, null)));

        var result = repository.findByUserIds(List.of(first, second));

        assertThat(result).containsOnlyKeys(first, second);
        var values = result.get(first);
        assertThat(values.calls()).isEqualTo(Long.MAX_VALUE);
        assertThat(values.tokens()).isEqualTo(34500);
        assertThat(values.modelCalls()).isEqualTo(10);
        assertThat(values.unknownTokenCalls()).isEqualTo(2);
        assertThat(values.knownCostCalls()).isEqualTo(9);
        assertThat(values.unknownCostCalls()).isEqualTo(3);
        assertThat(values.costUsd()).isEqualTo(new BigDecimal("0.0123456789123456789"));
        assertThat(result.get(second)).isEqualTo(new UserAiUsageRepository.Metrics(0, 25, 0, 0, 0, 0, BigDecimal.ZERO));
        var order = inOrder(hashes);
        byte[][] fields = Stream.of("calls", "tokens", "model_calls", "unknown_token_calls", "known_cost_calls",
                "unknown_cost_calls", "cost_usd").map(field -> field.getBytes(StandardCharsets.UTF_8)).toArray(byte[][]::new);
        order.verify(hashes).hMGet(("argos:usage:users:v1:" + first).getBytes(StandardCharsets.UTF_8), fields);
        order.verify(hashes).hMGet(("argos:usage:users:v1:" + second).getBytes(StandardCharsets.UTF_8), fields);
        verifyNoMoreInteractions(hashes);
        verify(redis).executePipelined(any(RedisCallback.class), eq(StringRedisSerializer.UTF_8));
    }

    @Test
    void shouldReturnZeroForAnEntirelyMissingHash() {
        var id = UUID.randomUUID();
        pipeline(List.of(Arrays.asList(null, null, null, null, null, null, null)));
        assertThat(repository.findByUserIds(List.of(id)).get(id))
                .isEqualTo(new UserAiUsageRepository.Metrics(0, 0, 0, 0, 0, 0, BigDecimal.ZERO));
    }

    @Test
    void shouldNotReadRedisForAnEmptyPage() {
        assertThat(repository.findByUserIds(List.of())).isEmpty();
        verifyNoInteractions(redis);
    }

    static Stream<Arguments> invalidMetrics() {
        var counters = IntStream.range(0, 6).boxed().flatMap(index -> Stream.of("", " ", "invalid",
                "-1", "1.5", "9223372036854775808", "NaN").map(value -> Arguments.of(index, value)));
        return Stream.concat(counters, Stream.of("", " ", "invalid", "-0.01", "NaN", "Infinity")
                .map(value -> Arguments.of(6, value)));
    }

    @ParameterizedTest
    @MethodSource("invalidMetrics")
    void shouldTranslateInvalidMetricsWithoutReturningPartialData(int index, String invalid) {
        var values = new ArrayList<>(List.of("1", "2", "3", "4", "5", "6", "0.01"));
        values.set(index, invalid);
        pipeline(List.of(List.of("1", "2", "3", "4", "5", "6", "0.01"), values));

        assertThatThrownBy(() -> repository.findByUserIds(List.of(UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(ArgosInternalServerException.class)
                .hasMessage("Não foi possível consultar o uso de inteligência artificial")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldPreserveCauseButSanitizeInfrastructureErrors() {
        when(redis.getStringSerializer()).thenReturn(StringRedisSerializer.UTF_8);
        var cause = new RedisConnectionFailureException("secret connection details");
        when(redis.executePipelined(any(RedisCallback.class), any(RedisSerializer.class))).thenThrow(cause);

        assertThatThrownBy(() -> repository.findByUserIds(List.of(UUID.randomUUID())))
                .isInstanceOf(ArgosInternalServerException.class)
                .hasMessage("Não foi possível consultar o uso de inteligência artificial").hasCause(cause);
    }

    private void pipeline(List<?> replies) {
        when(redis.getStringSerializer()).thenReturn(StringRedisSerializer.UTF_8);
        when(connection.hashCommands()).thenReturn(hashes);
        when(redis.executePipelined(any(RedisCallback.class), any(RedisSerializer.class))).thenAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            assertThat(callback.doInRedis(connection)).isNull();
            return replies;
        });
    }
}
