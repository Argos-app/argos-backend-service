package com.argos.backend_services.controller;

import com.argos.backend_services.config.SecurityConfig;
import com.argos.backend_services.controller.impl.HerdController;
import com.argos.backend_services.controller.impl.LotController;
import com.argos.backend_services.dto.HerdOut;
import com.argos.backend_services.entity.enums.HerdCategory;
import com.argos.backend_services.exception.NotFoundException;
import com.argos.backend_services.repository.UserRepository;
import com.argos.backend_services.security.CurrentUserData;
import com.argos.backend_services.service.HerdService;
import com.argos.backend_services.service.JwtService;
import com.argos.backend_services.service.LotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({LotController.class, HerdController.class})
@Import({SecurityConfig.class, JwtService.class})
@ActiveProfiles("test")
class LotRegistrationHttpTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean LotService lots;
    @MockBean HerdService herds;
    @MockBean UserRepository users;
    private final UUID userId = UUID.randomUUID();
    private final UUID propertyId = UUID.randomUUID();

    @ParameterizedTest
    @EnumSource(HerdCategory.class)
    void shouldAcceptHerdCategory(HerdCategory category) throws Exception {
        postLot(validBody().put("category", category.name())).andExpect(status().isOk());
        verify(lots).registerLot(argThat(input -> input.category() == category), eq(userId), eq(propertyId));
    }

    @Test
    void shouldAcceptExistingHerdWithoutNameOrBreedAndForwardContext() throws Exception {
        var herdId = UUID.randomUUID();
        var body = validBody().put("herdId", herdId.toString());
        body.remove(List.of("herdName", "breed"));

        postLot(body).andExpect(status().isOk()).andExpect(content().string(""));

        verify(lots).registerLot(argThat(input -> herdId.equals(input.herdId()) && input.herdName() == null
                && input.breed() == null), eq(userId), eq(propertyId));
    }

    @Test
    void shouldIgnoreAdditionalNameAndBreedWhenIdIsPresent() throws Exception {
        var body = validBody().put("herdId", UUID.randomUUID().toString())
                .put("herdName", "x".repeat(61)).put("breed", "x".repeat(41));
        postLot(body).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldAcceptOmittedOrNullHerdIdAndNormalizeNameAndBreed(boolean explicitNull) throws Exception {
        var body = validBody().put("herdName", "  Rebanho Nelore  ").put("breed", " Nelore ");
        if (explicitNull) body.putNull("herdId");

        postLot(body).andExpect(status().isOk());

        verify(lots).registerLot(argThat(input -> input.herdId() == null
                && input.herdName().equals("Rebanho Nelore") && input.breed().equals("Nelore")), eq(userId), eq(propertyId));
    }

    @Test
    void shouldAcceptExactLimitsAndZeroCount() throws Exception {
        postLot(validBody().put("herdName", "a".repeat(60)).put("breed", "a".repeat(40))
                .put("lotName", "a".repeat(20)).put("headCount", 0)).andExpect(status().isOk());
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("herdName", "null"), Arguments.of("herdName", "\"\""),
                Arguments.of("herdName", "\"   \""), Arguments.of("herdName", "\"" + "a".repeat(61) + "\""),
                Arguments.of("breed", "null"), Arguments.of("breed", "\"\""),
                Arguments.of("breed", "\"   \""), Arguments.of("breed", "\"" + "a".repeat(41) + "\""),
                Arguments.of("herdId", "\"invalid\""), Arguments.of("herdId", "1"),
                Arguments.of("headCount", "null"), Arguments.of("headCount", "-1"),
                Arguments.of("headCount", "\"invalid\""),
                Arguments.of("headCount", "2147483648"), Arguments.of("lotName", "\" \""),
                Arguments.of("lotName", "\"" + "a".repeat(21) + "\""),
                Arguments.of("category", "\"INVALID\""), Arguments.of("releaseSlaughterDate", "null")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void shouldUseJacksonDefaultForEmptyHerdId(String value) throws Exception {
        postLot(validBody().put("herdId", value)).andExpect(status().isOk());
        verify(lots).registerLot(argThat(input -> input.herdId() == null), eq(userId), eq(propertyId));
    }

    @Test
    void shouldUseJacksonDefaultForNumericText() throws Exception {
        postLot(validBody().put("headCount", "200")).andExpect(status().isOk());
        verify(lots).registerLot(argThat(input -> input.headCount() == 200), eq(userId), eq(propertyId));
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void shouldRejectInvalidInputBeforeCallingService(String field, String value) throws Exception {
        var body = validBody();
        body.set(field, json.readTree(value));
        postLot(body).andExpect(status().isBadRequest());
        verifyNoInteractions(lots);
    }

    @ParameterizedTest
    @ValueSource(strings = {"herdName", "breed", "lotName", "category", "headCount", "releaseSlaughterDate"})
    void shouldRejectMissingRequiredFields(String field) throws Exception {
        var body = validBody();
        body.remove(field);
        postLot(body).andExpect(status().isBadRequest());
        verifyNoInteractions(lots);
    }

    @Test
    void shouldReturn404AndMessageForUnknownHerd() throws Exception {
        doThrow(new NotFoundException("Rebanho não encontrado")).when(lots).registerLot(any(), eq(userId), eq(propertyId));
        postLot(validBody().put("herdId", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Rebanho não encontrado"));
    }

    @Test
    void shouldReturn403ForUnauthorizedProperty() throws Exception {
        doThrow(new AccessDeniedException("Sem acesso")).when(lots).registerLot(any(), eq(userId), eq(propertyId));
        postLot(validBody()).andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn409ForDuplicateName() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Já existe rebanho com esse nome na propriedade"))
                .when(lots).registerLot(any(), eq(userId), eq(propertyId));
        postLot(validBody()).andExpect(status().isConflict()).andExpect(jsonPath("$.message")
                .value("Já existe rebanho com esse nome na propriedade"));
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mvc.perform(post("/lot").contentType("application/json").content(validBody().toString()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/herd/list-all")).andExpect(status().isUnauthorized());
        verifyNoInteractions(lots, herds);
    }

    @Test
    void shouldReturnHerdIdAlongsideExistingFields() throws Exception {
        var herdId = UUID.randomUUID();
        when(herds.listHerd(userId, propertyId)).thenReturn(List.of(new HerdOut(herdId, propertyId, "Nelore", "Nelore", 200)));
        mvc.perform(get("/herd/list-all").with(authentication(identity())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].herdId").value(herdId.toString()))
                .andExpect(jsonPath("$[0].propertyId").value(propertyId.toString()))
                .andExpect(jsonPath("$[0].herdName").value("Nelore"))
                .andExpect(jsonPath("$[0].breed").value("Nelore"))
                .andExpect(jsonPath("$[0].headCount").value(200));
    }

    @Test
    void shouldReturnEmptyHerdList() throws Exception {
        when(herds.listHerd(userId, propertyId)).thenReturn(List.of());
        mvc.perform(get("/herd/list-all").with(authentication(identity())))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    private ObjectNode validBody() {
        return json.createObjectNode().put("lotName", "L-1").put("herdName", "Rebanho Nelore")
                .put("breed", "Nelore").put("category", "FATTENING").put("headCount", 200)
                .put("releaseSlaughterDate", "2027-07-01");
    }

    private org.springframework.test.web.servlet.ResultActions postLot(ObjectNode body) throws Exception {
        return mvc.perform(post("/lot").with(authentication(identity())).contentType("application/json").content(body.toString()));
    }

    private UsernamePasswordAuthenticationToken identity() {
        return new UsernamePasswordAuthenticationToken(new CurrentUserData(userId, "user@argos.com", "User", "USER", propertyId),
                null, List.of());
    }
}
