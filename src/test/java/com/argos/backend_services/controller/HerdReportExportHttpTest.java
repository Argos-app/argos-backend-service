package com.argos.backend_services.controller;

import com.argos.backend_services.config.SecurityConfig;
import com.argos.backend_services.config.WebConfig;
import com.argos.backend_services.controller.handler.CustomGlobalException;
import com.argos.backend_services.controller.impl.ReportController;
import com.argos.backend_services.controller.resolver.CurrentUserArgumentResolver;
import com.argos.backend_services.controller.resolver.FirebaseTokenArgumentResolver;
import com.argos.backend_services.dto.out.CurrentUserAccessOut;
import com.argos.backend_services.dto.out.HerdReportListItemOut;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Permission;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.export.HerdReportFixture;
import com.argos.backend_services.export.HerdReportXlsxExporter;
import com.argos.backend_services.repository.HerdReportRepository;
import com.argos.backend_services.repository.PropertyRepository;
import com.argos.backend_services.repository.UserRepository;
import com.argos.backend_services.security.JwtAuthenticationFilter;
import com.argos.backend_services.service.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = HerdReportExportHttpTest.Config.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HerdReportExportHttpTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean UserRepository users;
    @MockBean PropertyRepository properties;
    @MockBean HerdReportRepository reports;
    @MockBean ReportHerdService herdService;
    @MockBean ReportMedicineService medicineService;
    private final UUID administratorId = UUID.randomUUID();
    private final UUID farmId = UUID.randomUUID();

    @BeforeEach
    void resetMocks() {
        reset(users, properties, reports, herdService, medicineService);
    }

    @Test
    void shouldListReportMetadataWithFilterAndPaginationWithoutNestedCollections() throws Exception {
        var item = HerdReportListItemOut.from(HerdReportFixture.complete());
        when(herdService.list(farmId, 1, 2, administratorId))
                .thenReturn(new PageOut<>(List.of(item), 1, 2, 5, 3, false, false));
        mvc.perform(listRequest().param("propertyId", farmId.toString()).param("page", "1").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data[0]", org.hamcrest.Matchers.aMapWithSize(8)))
                .andExpect(jsonPath("$.data[0].id").value("report-1"))
                .andExpect(jsonPath("$.data[0].reportType").value("ON_DEMAND"))
                .andExpect(jsonPath("$.data[0].reportCreatedAt").isString())
                .andExpect(jsonPath("$.data[0].lastUpdatedAt").isString())
                .andExpect(jsonPath("$.data[0].propertyId").value("property-1"))
                .andExpect(jsonPath("$.data[0].propertyName").value("Fazenda São João"))
                .andExpect(jsonPath("$.data[0].herdName").value("Rebanho histórico"))
                .andExpect(jsonPath("$.data[0].herdBreed").value("Nelore"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));
        verify(herdService).list(farmId, 1, 2, administratorId);
        verifyNoInteractions(properties, reports, medicineService);
    }

    @Test
    void shouldListAnEmptyPageUsingDefaultPaginationWithoutAPropertyClaim() throws Exception {
        when(herdService.list(null, 0, 6, administratorId))
                .thenReturn(new PageOut<>(List.of(), 0, 6, 0, 0, true, true));
        mvc.perform(listRequest().header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(6))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
        verify(herdService).list(null, 0, 6, administratorId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Bearer invalid", "Basic invalid"})
    void shouldRejectUnauthenticatedReportListing(String authorization) throws Exception {
        mvc.perform(listRequest().header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(herdService);
    }

    @Test
    void shouldRejectReportListingWhenTheCurrentPermissionIsNotAdmin() throws Exception {
        mvc.perform(listRequest().header(HttpHeaders.AUTHORIZATION, bearer("APLICADOR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(herdService);
    }

    @Test
    void shouldReturnForbiddenWhenListingAForeignFarm() throws Exception {
        when(herdService.list(farmId, 0, 6, administratorId))
                .thenThrow(new AccessDeniedException("Usuário não administra a propriedade informada"));
        mvc.perform(listRequest().param("propertyId", farmId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @ParameterizedTest
    @CsvSource({"propertyId,not-a-uuid", "page,invalid", "size,invalid"})
    void shouldRejectMalformedListingParameters(String parameter, String value) throws Exception {
        mvc.perform(listRequest().param(parameter, value).header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(herdService);
    }

    private MockHttpServletRequestBuilder listRequest() {
        return get("/api/v1/reports/herds").contextPath("/api/v1").servletPath("/reports/herds")
                .accept(MediaType.APPLICATION_JSON);
    }

    @Test
    void shouldDownloadSelectedReportEvenWhenTokenHasNoPropertyId() throws Exception {
        var report = HerdReportFixture.complete();
        report.setRelationalPropertyId(farmId.toString());
        when(properties.findByUserIdAndActiveTrue(administratorId))
                .thenReturn(List.of(Property.builder().id(farmId).build()));
        when(reports.findByIdAndRelationalPropertyIdIn("report-1", List.of(farmId.toString())))
                .thenReturn(Optional.of(report));
        byte[] body = mvc.perform(request("report-1").header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio-rebanho.xlsx\""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andReturn().getResponse().getContentAsByteArray();
        try (var book = new XSSFWorkbook(new ByteArrayInputStream(body))) {
            assertThat(book.getNumberOfSheets()).isEqualTo(8);
            assertThat(book.getSheet("Resumo").getRow(1).getCell(0).getStringCellValue()).isEqualTo("report-1");
        }
        verifyNoInteractions(herdService, medicineService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Bearer invalid", "Basic invalid"})
    void shouldRejectMissingOrInvalidAuthentication(String authorization) throws Exception {
        mvc.perform(request("report-1").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(properties, reports);
    }

    @Test
    void shouldRejectExpiredAuthentication() throws Exception {
        var expiredJwt = new JwtService("test-secret-minimo-32-caracteres-para-hs256", -1000);
        mvc.perform(request("report-1").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + expiredJwt.generate(user("ADMIN"))))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(properties, reports);
    }

    @Test
    void shouldUseCurrentPermissionFromDatabaseInsteadOfOldAdminClaim() throws Exception {
        String token = bearer("APLICADOR");
        mvc.perform(request("report-1").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.informationCode").value("FORBIDDEN"));
        verifyNoInteractions(properties, reports);
    }

    @Test
    void shouldReturnNotFoundForInaccessibleReport() throws Exception {
        when(properties.findByUserIdAndActiveTrue(administratorId))
                .thenReturn(List.of(Property.builder().id(farmId).build()));
        when(reports.findByIdAndRelationalPropertyIdIn("foreign", List.of(farmId.toString())))
                .thenReturn(Optional.empty());
        mvc.perform(request("foreign").header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Relatório não encontrado"))
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void shouldReturnGenericServerErrorBeforeSendingAnAttachment() throws Exception {
        when(properties.findByUserIdAndActiveTrue(administratorId))
                .thenThrow(new DataAccessResourceFailureException("secret database credentials"));
        mvc.perform(request("report-1").header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Não foi possível exportar o relatório"))
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION));
    }

    private MockHttpServletRequestBuilder request(String reportId) {
        String path = "/reports/herds/" + reportId + "/export";
        return get("/api/v1" + path).contextPath("/api/v1").servletPath(path)
                .accept("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    private AppUser user(String permission) {
        return AppUser.builder().id(administratorId).name("Admin").email("admin@argos.test")
                .permission(Permission.builder().code(permission).build()).build();
    }

    private String bearer(String currentPermission) {
        when(users.findCurrentAccessById(administratorId)).thenReturn(Optional.of(new CurrentUserAccessOut() {
            public Boolean getUserActive() { return true; }
            public Boolean getPermissionActive() { return true; }
            public String getPermissionCode() { return currentPermission; }
        }));
        return "Bearer " + jwt.generate(user("ADMIN"));
    }

    @Configuration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, MongoAutoConfiguration.class,
            MongoDataAutoConfiguration.class})
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, ReportController.class, WebConfig.class,
            FirebaseTokenArgumentResolver.class, CurrentUserArgumentResolver.class, CustomGlobalException.class,
            HerdReportExportService.class, HerdReportXlsxExporter.class, JwtService.class})
    static class Config { }
}
