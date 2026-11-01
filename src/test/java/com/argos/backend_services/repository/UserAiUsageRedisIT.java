package com.argos.backend_services.repository;

import com.argos.backend_services.exception.ArgosInternalServerException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class UserAiUsageRedisIT {
    private static final String PASSWORD = "isolated-test-password";
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379).withCommand("redis-server", "--requirepass", PASSWORD);
    private static LettuceConnectionFactory connections;
    private static StringRedisTemplate redis;
    private static UserAiUsageRepository repository;

    @BeforeAll
    static void connect() {
        connections = connectionFactory(PASSWORD);
        redis = new StringRedisTemplate(connections);
        repository = new UserAiUsageRepository(redis);
    }

    @AfterAll
    static void disconnect() {
        if (connections != null) connections.destroy();
    }

    @Test
    void shouldReadActualHashesAndMissingFieldsInPipelineOrderWithoutWriting() {
        var first = UUID.randomUUID();
        var absent = UUID.randomUUID();
        var partial = UUID.randomUUID();
        var firstKey = key(first);
        Map<String, String> fields = Map.of("calls", "9223372036854775807", "tokens", "34500",
                "model_calls", "10", "unknown_token_calls", "2", "known_cost_calls", "9",
                "unknown_cost_calls", "3", "cost_usd", "0.0123456789123456789", "name", "Stale name",
                "email", "stale@example.com");
        redis.opsForHash().putAll(firstKey, fields);
        redis.expire(firstKey, Duration.ofMinutes(5));
        redis.opsForHash().put(key(partial), "tokens", "25");

        var result = repository.findByUserIds(List.of(partial, absent, first));

        assertThat(result.get(first)).isEqualTo(new UserAiUsageRepository.Metrics(Long.MAX_VALUE, 34500, 10, 2, 9, 3,
                new BigDecimal("0.0123456789123456789")));
        assertThat(result.get(partial)).isEqualTo(new UserAiUsageRepository.Metrics(0, 25, 0, 0, 0, 0, BigDecimal.ZERO));
        assertThat(result.get(absent)).isEqualTo(new UserAiUsageRepository.Metrics(0, 0, 0, 0, 0, 0, BigDecimal.ZERO));
        assertThat(redis.hasKey(key(absent))).isFalse();
        assertThat(redis.opsForHash().entries(firstKey)).containsExactlyInAnyOrderEntriesOf(fields);
        assertThat(redis.opsForHash().size(key(partial))).isEqualTo(1);
        assertThat(redis.getExpire(firstKey)).isBetween(1L, 300L);
    }

    @Test
    void shouldTranslateWrongKeyTypeWithoutPartialSuccess() {
        var id = UUID.randomUUID();
        redis.opsForValue().set(key(id), "not-a-hash");

        assertThatThrownBy(() -> repository.findByUserIds(List.of(UUID.randomUUID(), id)))
                .isInstanceOf(ArgosInternalServerException.class)
                .hasMessage("Não foi possível consultar o uso de inteligência artificial");
        assertThat(redis.opsForValue().get(key(id))).isEqualTo("not-a-hash");
    }

    @Test
    void shouldTranslateInvalidNumericDataReadFromARealHash() {
        var id = UUID.randomUUID();
        redis.opsForHash().put(key(id), "cost_usd", "NaN");
        assertThatThrownBy(() -> repository.findByUserIds(List.of(id)))
                .isInstanceOf(ArgosInternalServerException.class).hasCauseInstanceOf(NumberFormatException.class);
    }

    @Test
    void shouldTranslateAuthenticationFailure() {
        var invalidConnections = connectionFactory("wrong-test-password");
        try {
            var invalidRepository = new UserAiUsageRepository(new StringRedisTemplate(invalidConnections));
            assertThatThrownBy(() -> invalidRepository.findByUserIds(List.of(UUID.randomUUID())))
                    .isInstanceOf(ArgosInternalServerException.class)
                    .hasMessage("Não foi possível consultar o uso de inteligência artificial");
        } finally {
            invalidConnections.destroy();
        }
    }

    private static String key(UUID id) {
        return "argos:usage:users:v1:" + id;
    }

    private static LettuceConnectionFactory connectionFactory(String password) {
        var configuration = new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379));
        configuration.setPassword(RedisPassword.of(password));
        var client = LettuceClientConfiguration.builder().commandTimeout(Duration.ofSeconds(2)).build();
        var factory = new LettuceConnectionFactory(configuration, client);
        factory.afterPropertiesSet();
        return factory;
    }
}
