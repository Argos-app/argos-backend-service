package com.argos.backend_services.controller;

import com.argos.backend_services.config.SecurityConfig;
import com.argos.backend_services.controller.impl.UserController;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.dto.in.CreateUserIn;
import com.argos.backend_services.dto.in.UpdateCurrentUserIn;
import com.argos.backend_services.dto.out.CreateUserOut;
import com.argos.backend_services.dto.out.UserListItemOut;
import com.argos.backend_services.dto.out.UserPropertyOut;
import com.argos.backend_services.security.CurrentUserData;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.service.JwtService;
import com.argos.backend_services.service.UserService;
import com.argos.backend_services.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
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

    @MockBean
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
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAuthenticatedUserToUpdateOnlyProvidedProfileFields() throws Exception {
        UpdateCurrentUserIn input = new UpdateCurrentUserIn("Updated User", null, null);

        mockMvc.perform(patch("/users/update-perfil")
                        .with(authentication(userAuthentication()))
                        .contentType("application/json")
                        .content("{\"name\":\"Updated User\"}"))
                .andExpect(status().isNoContent());

        verify(userService).updateCurrentUser(org.mockito.ArgumentMatchers.any(UUID.class), org.mockito.ArgumentMatchers.eq(input));
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
}
