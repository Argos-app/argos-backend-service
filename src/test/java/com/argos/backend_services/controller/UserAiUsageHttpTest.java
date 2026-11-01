package com.argos.backend_services.controller;

import com.argos.backend_services.config.SecurityConfig;
import com.argos.backend_services.controller.impl.UserController;
import com.argos.backend_services.dto.out.CurrentUserAccessOut;
import com.argos.backend_services.dto.out.UserListItemOut;
import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Permission;
import com.argos.backend_services.exception.ArgosInternalServerException;
import com.argos.backend_services.repository.UserAiUsageRepository;
import com.argos.backend_services.repository.UserRepository;
import com.argos.backend_services.security.CurrentUserData;
import com.argos.backend_services.service.JwtService;
import com.argos.backend_services.service.UserAiUsageService;
import com.argos.backend_services.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtService.class, UserAiUsageService.class})
@ActiveProfiles("test")
class UserAiUsageHttpTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtService jwt;
    @MockBean UserService userService;
    @MockBean UserRepository users;
    @MockBean UserAiUsageRepository usage;
    private final UUID administratorId = UUID.randomUUID();

    @Test
    void shouldExposeExactPaginatedContractWithDatabaseIdentityAndNumericMetrics() throws Exception {
        var id = UUID.randomUUID();
        var item = new UserListItemOut(id, null, "Maria Oliveira", "12345678901", "maria@example.com",
                false, UUID.randomUUID());
        when(users.findLinkedByAdministrator(administratorId, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1));
        when(usage.findByUserIds(List.of(id))).thenReturn(Map.of(id,
                new UserAiUsageRepository.Metrics(Long.MAX_VALUE, 34500, 10, 2, 9, 3, new BigDecimal("0.01234567"))));

        var response = mvc.perform(get("/api/v1/users/ai-usage").contextPath("/api/v1").servletPath("/users/ai-usage")
                        .with(authentication(identity("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(id.toString()))
                .andExpect(jsonPath("$.data[0].name").value("Maria Oliveira"))
                .andExpect(jsonPath("$.data[0].email").value("maria@example.com"))
                .andExpect(jsonPath("$.data[0].photoUrl").value(""))
                .andExpect(jsonPath("$.data[0].calls").value(Long.MAX_VALUE))
                .andExpect(jsonPath("$.data[0].tokens").value(34500))
                .andExpect(jsonPath("$.data[0].modelCalls").value(10))
                .andExpect(jsonPath("$.data[0].unknownTokenCalls").value(2))
                .andExpect(jsonPath("$.data[0].known_cost_calls").value(9))
                .andExpect(jsonPath("$.data[0].unknown_cost_calls").value(3))
                .andExpect(jsonPath("$.data[0].cost_usd").isNumber())
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true)).andExpect(jsonPath("$.last").value(true))
                .andReturn().getResponse().getContentAsString();

        var root = json.readTree(response);
        assertThat(root.size()).isEqualTo(7);
        assertThat(root.path("data").get(0).size()).isEqualTo(11);
        assertThat(root.path("data").get(0).path("cost_usd").decimalValue())
                .isEqualByComparingTo("0.01234567");
        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @CsvSource({"0,1", "0,100", "3,10"})
    void shouldAcceptPaginationAndReturnEmptyPagesWithoutRedis(int page, int size) throws Exception {
        when(users.findLinkedByAdministrator(administratorId, PageRequest.of(page, size)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(page, size), 0));
        mvc.perform(get("/users/ai-usage").param("page", Integer.toString(page)).param("size", Integer.toString(size))
                        .with(authentication(identity("ADMIN"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.page").value(page)).andExpect(jsonPath("$.size").value(size))
                .andExpect(jsonPath("$.totalElements").value(0));
        verifyNoInteractions(usage);
    }

    @ParameterizedTest
    @CsvSource({"-1,10", "0,0", "0,-1", "0,101", "abc,10", "0,abc", "1.5,10", "0,1.5",
            "2147483648,10", "0,2147483648"})
    void shouldRejectInvalidPaginationBeforeReadingUsersOrRedis(String page, String size) throws Exception {
        mvc.perform(get("/users/ai-usage").param("page", page).param("size", size)
                        .with(authentication(identity("ADMIN"))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(users, usage);
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mvc.perform(get("/users/ai-usage")).andExpect(status().isUnauthorized());
        verifyNoInteractions(users, usage);
    }

    @Test
    void shouldForbidNonAdminWithoutReadingTheList() throws Exception {
        mvc.perform(get("/users/ai-usage").with(authentication(identity("USER"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(users, usage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "expired"})
    void shouldRejectInvalidOrExpiredJwt(String kind) throws Exception {
        var token = kind.equals("invalid") ? "invalid" : new JwtService(
                "test-secret-minimo-32-caracteres-para-hs256", -1000).generate(account());
        mvc.perform(get("/users/ai-usage").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(users, usage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "inactive-user", "inactive-permission"})
    void shouldRejectJwtWhenCurrentAccessIsNoLongerActive(String reason) throws Exception {
        if (reason.equals("missing")) {
            when(users.findCurrentAccessById(administratorId)).thenReturn(Optional.empty());
        } else {
            var access = mock(CurrentUserAccessOut.class);
            when(access.getUserActive()).thenReturn(!reason.equals("inactive-user"));
            if (!reason.equals("inactive-user")) when(access.getPermissionActive()).thenReturn(false);
            when(users.findCurrentAccessById(administratorId)).thenReturn(Optional.of(access));
        }
        mvc.perform(get("/users/ai-usage").header("Authorization", "Bearer " + jwt.generate(account())))
                .andExpect(status().isUnauthorized());
        verify(users).findCurrentAccessById(administratorId);
        verifyNoMoreInteractions(users);
        verifyNoInteractions(usage);
    }

    @Test
    void shouldUseCurrentDatabasePermissionAndJwtIdentityForAnAuthorizedRequest() throws Exception {
        var access = mock(CurrentUserAccessOut.class);
        when(access.getUserActive()).thenReturn(true);
        when(access.getPermissionActive()).thenReturn(true);
        when(access.getPermissionCode()).thenReturn("ADMIN");
        when(users.findCurrentAccessById(administratorId)).thenReturn(Optional.of(access));
        when(users.findLinkedByAdministrator(administratorId, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        var account = account();
        account.setPermission(Permission.builder().code("USER").name("User").build());
        mvc.perform(get("/users/ai-usage").header("Authorization", "Bearer " + jwt.generate(account)))
                .andExpect(status().isOk());
        verify(users).findLinkedByAdministrator(administratorId, PageRequest.of(0, 10));
        verifyNoInteractions(usage);
    }

    @Test
    void shouldReturnSanitizedIntegrationFailureInsteadOfZeroUsage() throws Exception {
        var item = new UserListItemOut(UUID.randomUUID(), "", "User", "12345678901", "user@example.com",
                true, UUID.randomUUID());
        when(users.findLinkedByAdministrator(administratorId, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1));
        when(usage.findByUserIds(anyList())).thenThrow(new ArgosInternalServerException(HttpStatus.INTERNAL_SERVER_ERROR,
                "ARGOS_INTERNAL_SERVER_ERROR", "Não foi possível consultar o uso de inteligência artificial"));

        mvc.perform(get("/users/ai-usage").with(authentication(identity("ADMIN"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Não foi possível consultar o uso de inteligência artificial"))
                .andExpect(jsonPath("$.code").value("500 INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.informationCode").value("ARGOS_INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private UsernamePasswordAuthenticationToken identity(String permission) {
        return new UsernamePasswordAuthenticationToken(new CurrentUserData(administratorId, "admin@example.com",
                "Admin", permission, UUID.randomUUID()), null, List.of(new SimpleGrantedAuthority(permission)));
    }

    private AppUser account() {
        return AppUser.builder().id(administratorId).name("Admin").email("admin@example.com")
                .permission(Permission.builder().code("ADMIN").name("Admin").build()).build();
    }
}
