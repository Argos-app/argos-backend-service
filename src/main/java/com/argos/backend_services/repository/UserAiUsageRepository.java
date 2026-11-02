package com.argos.backend_services.repository;

import com.argos.backend_services.exception.ArgosInternalServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserAiUsageRepository {
    private static final String KEY_PREFIX = "argos:usage:users:v1:";
    private static final byte[][] FIELDS = Stream.of("calls", "tokens", "model_calls", "unknown_token_calls",
                    "known_cost_calls", "unknown_cost_calls", "cost_usd")
            .map(field -> field.getBytes(StandardCharsets.UTF_8)).toArray(byte[][]::new);

    private final StringRedisTemplate redis;

    public Map<UUID, Metrics> findByUserIds(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        try {
            List<Object> replies = redis.executePipelined((RedisCallback<Object>) connection -> {
                for (UUID userId : userIds) {
                    connection.hashCommands().hMGet((KEY_PREFIX + userId).getBytes(StandardCharsets.UTF_8), FIELDS);
                }
                return null;
            }, redis.getStringSerializer());

            Map<UUID, Metrics> result = new HashMap<>();
            for (int i = 0; i < userIds.size(); i++) {
                List<?> values = (List<?>) replies.get(i);
                result.put(userIds.get(i), new Metrics(counter(values.get(0)), counter(values.get(1)),
                        counter(values.get(2)), counter(values.get(3)), counter(values.get(4)),
                        counter(values.get(5)), cost(values.get(6))));
            }
            return result;

        } catch (DataAccessException | IllegalArgumentException exception) {
            log.error("ai_usage_read_failed reason={}", exception.getClass().getSimpleName());
            var failure = new ArgosInternalServerException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "ARGOS_INTERNAL_SERVER_ERROR", "Não foi possível consultar o uso de inteligência artificial");
            failure.initCause(exception);
            throw failure;
        }
    }

    private static long counter(Object value) {
        long result = value == null ? 0 : Long.parseLong((String) value);
        if (result < 0) {
            throw new IllegalArgumentException("Contagem negativa");
        }
        return result;
    }

    private static BigDecimal cost(Object value) {
        BigDecimal result = value == null ? BigDecimal.ZERO : new BigDecimal((String) value);
        if (result.signum() < 0) {
            throw new IllegalArgumentException("Custo de uso negativo");
        }
        return result;
    }

    public record Metrics(long calls, long tokens, long modelCalls, long unknownTokenCalls,
                          long knownCostCalls, long unknownCostCalls, BigDecimal costUsd) {}
}
