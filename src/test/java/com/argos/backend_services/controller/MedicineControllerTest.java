package com.argos.backend_services.controller;

import com.argos.backend_services.config.SecurityConfig;
import com.argos.backend_services.controller.impl.MedicineController;
import com.argos.backend_services.dto.in.MedicineBatchIn;
import com.argos.backend_services.dto.out.MedicineCreatedOut;
import com.argos.backend_services.dto.out.MedicineCatalogItemOut;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.repository.UserRepository;
import com.argos.backend_services.security.CurrentUserData;
import com.argos.backend_services.service.JwtService;
import com.argos.backend_services.service.MedicineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MedicineController.class)
@Import({SecurityConfig.class, JwtService.class})
@TestPropertySource(properties = {
        "security.jwt.secret=test-secret-minimo-32-caracteres-para-hs256",
        "security.jwt.expiration-ms=86400000"
})
class MedicineControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;
    @MockBean private MedicineService medicineService;
    @MockBean private UserRepository userRepository;

    @Test
    void shouldReturnAuthenticatedPaginatedCatalogWithDefaultPageAndSize() throws Exception {
        var item = new MedicineCatalogItemOut(UUID.randomUUID(), "Vacina A", "MAPA-123");
        when(medicineService.listCatalog(0, 20))
                .thenReturn(new PageOut<>(List.of(item), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/medicines/catalog").with(authentication(userAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].medicineId").value(item.medicineId().toString()))
                .andExpect(jsonPath("$.data[0].medicineName").value("Vacina A"))
                .andExpect(jsonPath("$.data[0].productRegistration").value("MAPA-123"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        verify(medicineService).listCatalog(0, 20);
        verifyNoMoreInteractions(medicineService);
    }

    @Test
    void shouldRegisterBatchWithoutPropertyIdInRequestBody() throws Exception {
        var input = new MedicineBatchIn("MAPA-123", "LOTE-001",
                java.time.LocalDate.of(2026, 1, 10), java.time.LocalDate.of(2027, 1, 10),
                java.math.BigDecimal.TEN, "Fornecedor", "12345678000199");
        var batchId = UUID.randomUUID();
        when(medicineService.registerBatch(input, USER_ID)).thenReturn(new MedicineCreatedOut(batchId));

        mockMvc.perform(post("/medicines").with(authentication(userAuthentication()))
                        .contentType("application/json")
                        .content("""
                                {"productRegistration":"MAPA-123","batchNumber":"LOTE-001",
                                 "manufactureDate":"2026-01-10","expiryDate":"2027-01-10",
                                 "quantity":10,"supplierName":"Fornecedor","supplierCnpj":"12345678000199"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicineBatchId").value(batchId.toString()));

        verify(medicineService).registerBatch(input, USER_ID);
    }
    @Test
    void shouldRejectInvalidPaginationParameters() throws Exception {
        when(medicineService.listCatalog(-1, 20)).thenThrow(new BadRequestException("Página não pode ser negativa"));

        mockMvc.perform(get("/medicines/catalog").param("page", "-1")
                        .with(authentication(userAuthentication())))
                .andExpect(status().isBadRequest());

        verify(medicineService).listCatalog(-1, 20);
    }

    @Test
    void shouldRequireAuthenticationToReadCatalog() throws Exception {
        mockMvc.perform(get("/medicines/catalog")).andExpect(status().isUnauthorized());
        verifyNoInteractions(medicineService);
    }

    private static UsernamePasswordAuthenticationToken userAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new CurrentUserData(USER_ID, "user@argos.com", "Argos User", "USER", null),
                null,
                List.of(new SimpleGrantedAuthority("USER")));
    }
}
