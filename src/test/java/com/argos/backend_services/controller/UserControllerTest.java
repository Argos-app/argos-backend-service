package com.argos.backend_services.controller;

import com.argos.backend_services.config.SecurityConfig;
import com.argos.backend_services.controller.impl.UserController;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.dto.in.CreateUserIn;
import com.argos.backend_services.dto.in.UpdateCurrentUserIn;
import com.argos.backend_services.dto.out.CreateUserOut;
import com.argos.backend_services.dto.out.UserListItemOut;
import com.argos.backend_services.dto.out.UserPropertyOut;
import com.argos.backend_services.dto.out.UserProfileOut;
import com.argos.backend_services.dto.out.CurrentUserAccessOut;
import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Permission;
import com.argos.backend_services.security.CurrentUserData;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.service.JwtService;
import com.argos.backend_services.service.UserService;
import com.argos.backend_services.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtService.class})
@TestPropertySource(properties = {
        "security.jwt.secret=test-secret-minimo-32-caracteres-para-hs256",
        "security.jwt.expiration-ms=86400000"
})
public class UserControllerTest {
    private static final UUID ADMINISTRATOR_ID = UUID.randomUUID();

    private static UsernamePasswordAuthenticationToken administratorAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new CurrentUserData(ADMINISTRATOR_ID, "admin@argos.com", "Administrator", "ADMIN", null),
                null, List.of(ADMIN_AUTHORITY));
    }

    private static UsernamePasswordAuthenticationToken userAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new CurrentUserData(UUID.randomUUID(), "user@argos.com", "User", "USER", null),
                null, List.of(USER_AUTHORITY));
    }

    private static final SimpleGrantedAuthority ADMIN_AUTHORITY =
            new SimpleGrantedAuthority("ADMIN");
    private static final SimpleGrantedAuthority USER_AUTHORITY =
            new SimpleGrantedAuthority("USER");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void shouldCreateUserForAdmin() throws Exception {
        UUID userId = UUID.randomUUID();
        CreateUserIn input = new CreateUserIn("New User", "new@argos.com", "12345678901",
                "password123", UUID.randomUUID(), UUID.randomUUID());
        when(userService.create(input, ADMINISTRATOR_ID)).thenReturn(new CreateUserOut(userId, "New User", "new@argos.com"));

        mockMvc.perform(post("/users")
                        .with(authentication(administratorAuthentication()))
                        .contentType("application/json")
                        .content("""
                                {"name":"New User","email":"new@argos.com","cpf":"12345678901",
                                 "password":"password123","permissionId":"%s","propertyId":"%s"}
                                """.formatted(input.permissionId(), input.propertyId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("New User"))
                .andExpect(jsonPath("$.email").value("new@argos.com"));

        verify(userService).create(input, ADMINISTRATOR_ID);
    }

    @Test
    void shouldForbidUserCreationForNonAdmin() throws Exception {
        mockMvc.perform(post("/users")
                        .with(authentication(userAuthentication()))
                        .contentType("application/json")
                        .content("""
                                {"name":"New User","email":"new@argos.com","cpf":"12345678901",
                                 "password":"password123","permissionId":"%s","propertyId":"%s"}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAuthenticatedUserToUpdateOnlyProvidedProfileFields() throws Exception {
        UpdateCurrentUserIn input = new UpdateCurrentUserIn("Updated User", null, null, null);

        mockMvc.perform(patch("/users/update-profile")
                        .with(authentication(userAuthentication()))
                        .contentType("application/json")
                        .content("{\"name\":\"Updated User\"}"))
                .andExpect(status().isNoContent());

        verify(userService).updateCurrentUser(org.mockito.ArgumentMatchers.any(UUID.class), org.mockito.ArgumentMatchers.eq(input));
    }

    @Test
    void shouldAcceptAddressOnlyForAuthenticatedUser() throws Exception {
        UpdateCurrentUserIn input = new UpdateCurrentUserIn(null, null, null, "a".repeat(255));

        mockMvc.perform(patch("/users/update-profile")
                        .with(authentication(administratorAuthentication()))
                        .contentType("application/json")
                        .content("{\"address\":\"" + input.address() + "\"}"))
                .andExpect(status().isNoContent());

        verify(userService).updateCurrentUser(ADMINISTRATOR_ID, input);
    }

    @Test
    void shouldAcceptAddressWithPersonalFields() throws Exception {
        var input = new UpdateCurrentUserIn("Updated User", "updated@argos.com", "https://example.com/photo.png", "Test Street");

        mockMvc.perform(patch("/users/update-profile")
                        .with(authentication(administratorAuthentication()))
                        .contentType("application/json")
                        .content("""
                                {"name":"Updated User","email":"updated@argos.com",
                                 "photoUrl":"https://example.com/photo.png","address":"Test Street"}
                                """))
                .andExpect(status().isNoContent());

        verify(userService).updateCurrentUser(ADMINISTRATOR_ID, input);
    }

    @Test
    void shouldRejectAddressOverColumnLimitBeforeCallingService() throws Exception {
        mockMvc.perform(patch("/users/update-profile")
                        .with(authentication(administratorAuthentication()))
                        .contentType("application/json")
                        .content("{\"address\":\"" + "a".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldRejectUnauthenticatedAddressUpdate() throws Exception {
        mockMvc.perform(patch("/users/update-profile")
                        .contentType("application/json")
                        .content("{\"address\":\"Test Street\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturnForbiddenWhenServiceRejectsAddressPermission() throws Exception {
        var input = new UpdateCurrentUserIn(null, null, null, "Test Street");
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("Sem permissão"))
                .when(userService).updateCurrentUser(ADMINISTRATOR_ID, input);

        mockMvc.perform(patch("/users/update-profile")
                        .with(authentication(administratorAuthentication()))
                        .contentType("application/json")
                        .content("{\"address\":\"Test Street\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.informationCode").value("FORBIDDEN"));
    }

    @Test
    void shouldReturnPaginatedUsersForAdmin() throws Exception {
        UserListItemOut item = new UserListItemOut(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "https://example.com/photo.jpg",
                "Argos User",
                "12345678901",
                "user@argos.com",
                true,
                UUID.randomUUID(),
                List.of(new UserPropertyOut(UUID.randomUUID(), "Fazenda Sao Joao", "12345678000190", LocalDate.of(2026, 1, 15)))
        );
        PageOut<UserListItemOut> pageOut = new PageOut<>(
                List.of(item), 0, 6, 7, 2, true, false
        );
        when(userService.listUsers(0, 6, ADMINISTRATOR_ID)).thenReturn(pageOut);

        mockMvc.perform(get("/users").with(authentication(administratorAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.data[0].photoUrl").value("https://example.com/photo.jpg"))
                .andExpect(jsonPath("$.data[0].name").value("Argos User"))
                .andExpect(jsonPath("$.data[0].cpf").value("12345678901"))
                .andExpect(jsonPath("$.data[0].email").value("user@argos.com"))
                .andExpect(jsonPath("$.data[0].active").value(true))
                .andExpect(jsonPath("$.data[0].properties[0].name").value("Fazenda Sao Joao"))
                .andExpect(jsonPath("$.data[0].properties[0].cnpj").value("12345678000190"))
                .andExpect(jsonPath("$.data[0].properties[0].linkDate").value("2026-01-15"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(6))
                .andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void shouldReturnForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/users").with(authentication(userAuthentication())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnBadRequestForNegativePage() throws Exception {
        when(userService.listUsers(-1, 6, ADMINISTRATOR_ID)).thenThrow(new BadRequestException("Página não pode ser negativa"));

        mockMvc.perform(get("/users").param("page", "-1")
                        .with(authentication(administratorAuthentication())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(HttpStatus.BAD_REQUEST.toString()))
                .andExpect(jsonPath("$.informationCode").value("BAD_REQUEST"));
    }

    private String profileToken(UUID userId, String code) {
        return jwtService.generate(AppUser.builder().id(userId).name("Old claim").email("old@test.invalid")
                .permission(Permission.builder().code(code).name(code).build()).build());
    }

    private CurrentUserAccessOut currentAccess(boolean accountActive, boolean permissionActive, String code) {
        return new CurrentUserAccessOut() {
            public Boolean getUserActive() { return accountActive; }
            public Boolean getPermissionActive() { return permissionActive; }
            public String getPermissionCode() { return code; }
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "ADMIN"})
    void shouldReadOnlyThePrincipalProfileRegardlessOfClientId(String code) throws Exception {
        UUID userId = UUID.randomUUID();
        when(userRepository.findCurrentAccessById(userId)).thenReturn(Optional.of(currentAccess(true, true, code)));
        when(userService.getMyProfile(userId)).thenReturn(new UserProfileOut(userId, "Current name",
                "current@test.invalid", "12345678901", null, "Current permission", code, true, List.of()));

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + profileToken(userId, code))
                        .param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Current name"))
                .andExpect(jsonPath("$.email").value("current@test.invalid"))
                .andExpect(jsonPath("$.permissionCode").value(code))
                .andExpect(jsonPath("$.properties").isEmpty());
        verify(userService).getMyProfile(userId);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void shouldRequireAuthenticationForProfile() throws Exception {
        mockMvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    }

    @Test
    void shouldRejectInvalidProfileToken() throws Exception {
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    }

    @Test
    void shouldRejectExpiredProfileToken() throws Exception {
        String token = new JwtService("test-secret-minimo-32-caracteres-para-hs256", -1000)
                .generate(AppUser.builder().id(UUID.randomUUID()).name("Test user").email("user@test.invalid")
                        .permission(Permission.builder().code("USER").name("Operador").build()).build());

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(userRepository, userService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "account-inactive", "permission-inactive"})
    void shouldRejectRevokedProfileAccess(String reason) throws Exception {
        UUID userId = UUID.randomUUID();
        when(userRepository.findCurrentAccessById(userId)).thenReturn(reason.equals("missing") ? Optional.empty()
                : Optional.of(currentAccess(!reason.equals("account-inactive"), !reason.equals("permission-inactive"), "USER")));

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + profileToken(userId, "USER")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    }

    @Test
    void shouldPreserveAdministrativeRestrictionsWithUserToken() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userRepository.findCurrentAccessById(userId)).thenReturn(Optional.of(currentAccess(true, true, "USER")));

        mockMvc.perform(get("/users").header("Authorization", "Bearer " + profileToken(userId, "USER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    }
}
