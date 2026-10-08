package com.argos.backend_services.security;

import com.argos.backend_services.config.SecurityConfig;
import com.argos.backend_services.config.WebConfig;
import com.argos.backend_services.controller.AuthController;
import com.argos.backend_services.controller.handler.CustomGlobalException;
import com.argos.backend_services.controller.resolver.CurrentUserArgumentResolver;
import com.argos.backend_services.controller.resolver.FirebaseTokenArgumentResolver;
import com.argos.backend_services.service.AuthService;
import com.argos.backend_services.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = SignInHttpSecurityTest.TestConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignInHttpSecurityTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void shouldPreserveMethodNotAllowedInsteadOfMaskingItAsUnauthorized() throws Exception {
        mvc.perform(request(HttpMethod.GET, "/api/v1/auth/signin")
                .contextPath("/api/v1").servletPath("/auth/signin"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldAllowErrorDispatchToPreserveOriginalHttpStatus() throws Exception {
        mvc.perform(request(HttpMethod.GET, "/api/v1/error")
                .contextPath("/api/v1").servletPath("/error")
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 405)
                .with(request -> {
                    request.setDispatcherType(DispatcherType.ERROR);
                    return request;
                }))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldReachPublicSignInAndRejectMissingFirebaseHeader() throws Exception {
        mvc.perform(request(HttpMethod.POST, "/api/v1/auth/signin")
                .contextPath("/api/v1").servletPath("/auth/signin"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.informationCode").value("BAD_REQUEST"));
    }

    @Test
    void shouldStillRequireAuthenticationForProtectedRoutes() throws Exception {
        mvc.perform(request(HttpMethod.GET, "/api/v1/auth/first-access")
                .contextPath("/api/v1").servletPath("/auth/first-access"))
                .andExpect(status().isUnauthorized());
    }

    @Configuration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, AuthController.class,
            WebConfig.class, FirebaseTokenArgumentResolver.class, CurrentUserArgumentResolver.class,
            CustomGlobalException.class, SignInRequestLoggingFilter.class})
    static class TestConfig {
        @Bean
        JwtService jwtService() {
            return new JwtService("test-secret-minimo-32-caracteres-para-hs256", 86400000);
        }

        @Bean
        AuthService authService() {
            return mock(AuthService.class);
        }
    }
}
