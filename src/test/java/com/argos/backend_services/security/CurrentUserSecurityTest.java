package com.argos.backend_services.security;

import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.controller.resolver.CurrentUserArgumentResolver;
import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Permission;
import com.argos.backend_services.service.JwtService;
import com.argos.backend_services.repository.UserRepository;
import com.argos.backend_services.dto.out.CurrentUserAccessOut;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserSecurityTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldExposeJwtClaimsAsCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        AppUser user = AppUser.builder()
                .id(userId)
                .email("user@argos.com")
                .name("Argos User")
                .permission(Permission.builder().code("USER").name("User").build())
                .build();
        JwtService jwtService = new JwtService(
                "test-secret-minimo-32-caracteres-para-hs256",
                86_400_000
        );
        String token = jwtService.generate(user);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserAccessOut access = new CurrentUserAccessOut() {
            public Boolean getUserActive() { return true; }
            public Boolean getPermissionActive() { return true; }
            public String getPermissionCode() { return "USER"; }
        };
        when(userRepository.findCurrentAccessById(userId)).thenReturn(java.util.Optional.of(access));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/properties");
        request.setServletPath("/properties");
        request.addHeader("Authorization", "Bearer " + token);

        new JwtAuthenticationFilter(jwtService, userRepository).doFilter(
                request,
                new MockHttpServletResponse(),
                (servletRequest, servletResponse) -> { }
        );

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertTrue(principal instanceof CurrentUserData);

        CurrentUserData currentUser = (CurrentUserData) principal;
        assertEquals(userId, currentUser.id());
        assertEquals("user@argos.com", currentUser.email());
        assertEquals("Argos User", currentUser.name());
        assertEquals("USER", currentUser.permission());

        CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();
        Method method = TestController.class.getDeclaredMethod("handler", CurrentUserData.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        assertTrue(resolver.supportsParameter(parameter));
        assertSame(currentUser, resolver.resolveArgument(
                parameter,
                new ModelAndViewContainer(),
                new org.springframework.web.context.request.ServletWebRequest(request),
                null
        ));
    }

    private static class TestController {

        void handler(@CurrentUser CurrentUserData currentUser) {
        }
    }
}
