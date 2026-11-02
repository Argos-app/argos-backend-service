package com.argos.backend_services.controller;

import com.argos.backend_services.config.SecurityConfig;
import com.argos.backend_services.config.WebConfig;
import com.argos.backend_services.controller.handler.CustomGlobalException;
import com.argos.backend_services.controller.impl.UserController;
import com.argos.backend_services.controller.resolver.CurrentUserArgumentResolver;
import com.argos.backend_services.controller.resolver.FirebaseTokenArgumentResolver;
import com.argos.backend_services.repository.UserAiUsageRepository;
import com.argos.backend_services.repository.UserRepository;
import com.argos.backend_services.security.JwtAuthenticationFilter;
import com.argos.backend_services.service.JwtService;
import com.argos.backend_services.service.UserAiUsageService;
import com.argos.backend_services.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = UserAiUsageOpenApiTest.TestConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAiUsageOpenApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired RedisProperties properties;
    @Autowired StringRedisTemplate redis;
    @MockBean UserService userService;
    @MockBean UserAiUsageRepository usage;
    @MockBean UserRepository users;

    @Test
    void shouldDocumentThePaginatedSchemaAndAllHttpStatusesWithoutInfrastructureConnections() throws Exception {
        var body = mvc.perform(get("/api/v1/v3/api-docs").contextPath("/api/v1").servletPath("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var document = json.readTree(body);
        var operation = document.path("paths").path("/users/ai-usage").path("get");
        assertThat(operation.isMissingNode()).isFalse();
        for (var status : new String[]{"200", "400", "401", "403", "500"}) {
            assertThat(operation.path("responses").has(status)).as("HTTP %s", status).isTrue();
        }
        var schema = operation.path("responses").path("200").path("content").path("*/*").path("schema").path("$ref").asText();
        assertThat(schema).startsWith("#/components/schemas/");
        var schemas = document.path("components").path("schemas");
        var page = schemas.path(schema.substring(schema.lastIndexOf('/') + 1)).path("properties");
        assertThat(page.size()).isEqualTo(7);
        var itemRef = page.path("data").path("items").path("$ref").asText();
        var fields = schemas.path(itemRef.substring(itemRef.lastIndexOf('/') + 1)).path("properties");
        var names = new ArrayList<String>();
        fields.fieldNames().forEachRemaining(names::add);
        assertThat(names).containsExactlyInAnyOrder("userId", "name", "email", "photoUrl", "calls", "tokens",
                "modelCalls", "unknownTokenCalls", "known_cost_calls", "unknown_cost_calls", "cost_usd");
        assertThat(fields.path("calls").path("format").asText()).isEqualTo("int64");
        assertThat(fields.path("cost_usd").path("type").asText()).isEqualTo("number");
        assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.getTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(redis.getHashValueSerializer()).isEqualTo(redis.getStringSerializer());
    }

    @Configuration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, MongoAutoConfiguration.class,
            MongoDataAutoConfiguration.class, MongoRepositoriesAutoConfiguration.class})
    @Import({UserController.class, UserAiUsageService.class, SecurityConfig.class, JwtService.class,
            JwtAuthenticationFilter.class, WebConfig.class, CurrentUserArgumentResolver.class,
            FirebaseTokenArgumentResolver.class, CustomGlobalException.class})
    static class TestConfig {}
}
