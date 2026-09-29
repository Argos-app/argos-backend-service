package com.argos.backend_services.controller;

import com.argos.backend_services.config.SecurityConfig;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.dto.out.UserListItemOut;
import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.service.JwtService;
import com.argos.backend_services.service.UserService;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
public class UserControllerTest {

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

    @Test
    void shouldReturnPaginatedUsersForAdmin() throws Exception {
        UserListItemOut item = new UserListItemOut(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "https://example.com/photo.jpg",
                "Argos User",
                "user@argos.com",
                true,
                "Fazenda Sao Joao",
                "12345678000190",
                LocalDate.of(2026, 1, 15)
        );
        PageOut<UserListItemOut> pageOut = new PageOut<>(
                List.of(item), 0, 6, 7, 2, true, false
        );
        when(userService.listUsers(0)).thenReturn(pageOut);

        mockMvc.perform(get("/users").with(user("admin").authorities(ADMIN_AUTHORITY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.content[0].photoUrl").value("https://example.com/photo.jpg"))
                .andExpect(jsonPath("$.content[0].name").value("Argos User"))
                .andExpect(jsonPath("$.content[0].email").value("user@argos.com"))
                .andExpect(jsonPath("$.content[0].active").value(true))
                .andExpect(jsonPath("$.content[0].farmName").value("Fazenda Sao Joao"))
                .andExpect(jsonPath("$.content[0].cnpj").value("12345678000190"))
                .andExpect(jsonPath("$.content[0].linkDate").value("2026-01-15"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(6))
                .andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void shouldReturnForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/users").with(user("user").authorities(USER_AUTHORITY)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnBadRequestForNegativePage() throws Exception {
        when(userService.listUsers(-1)).thenThrow(new BadRequestException("Página não pode ser negativa"));

        mockMvc.perform(get("/users").param("page", "-1")
                        .with(user("admin").authorities(ADMIN_AUTHORITY)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(HttpStatus.BAD_REQUEST.toString()))
                .andExpect(jsonPath("$.informationCode").value("BAD_REQUEST"));
    }
}
