package com.argos.backend_services.service;

import com.argos.backend_services.entity.HerdReport;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.exception.ArgosInternalServerException;
import com.argos.backend_services.exception.NotFoundException;
import com.argos.backend_services.export.HerdReportXlsxExporter;
import com.argos.backend_services.repository.HerdReportRepository;
import com.argos.backend_services.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HerdReportExportServiceTest {
    @Mock PropertyRepository properties;
    @Mock HerdReportRepository reports;
    @Mock HerdReportXlsxExporter exporter;
    @InjectMocks HerdReportExportService service;
    private final UUID administrator = UUID.randomUUID();

    @Test
    void shouldExportRequestedHistoricalReportFromAnyOwnedActiveFarm() throws Exception {
        var first = Property.builder().id(UUID.randomUUID()).build();
        var second = Property.builder().id(UUID.randomUUID()).build();
        var ids = List.of(first.getId().toString(), second.getId().toString());
        when(properties.findByUserIdAndActiveTrue(administrator)).thenReturn(List.of(first, second));
        for (var propertyId : ids) {
            var report = HerdReport.builder().id("old-" + propertyId).relationalPropertyId(propertyId).build();
            when(reports.findByIdAndRelationalPropertyIdIn(report.getId(), ids)).thenReturn(Optional.of(report));
            when(exporter.export(report)).thenReturn(new byte[]{1, 2});
            assertThat(service.export(report.getId(), administrator)).containsExactly(1, 2);
            verify(exporter).export(report);
        }
        verify(reports, never()).findById(anyString());
        verify(reports, never()).findByHerd_Id(anyString());
        verify(reports, never()).save(any());
    }

    @Test
    void shouldReturnNotFoundWithoutQueryingMongoWhenNoActiveOwnedFarmsExist() {
        when(properties.findByUserIdAndActiveTrue(administrator)).thenReturn(List.of());
        assertThatThrownBy(() -> service.export("foreign-or-inactive", administrator))
                .isInstanceOf(NotFoundException.class).hasMessage("Relatório não encontrado");
        verifyNoInteractions(reports, exporter);
    }

    @Test
    void shouldReturnSameNotFoundForMissingForeignOrInactiveReports() {
        var property = Property.builder().id(UUID.randomUUID()).build();
        when(properties.findByUserIdAndActiveTrue(administrator)).thenReturn(List.of(property));
        when(reports.findByIdAndRelationalPropertyIdIn("unavailable", List.of(property.getId().toString())))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.export("unavailable", administrator))
                .isInstanceOf(NotFoundException.class).hasMessage("Relatório não encontrado");
        verifyNoInteractions(exporter);
    }

    @Test
    void shouldHidePropertyDatabaseFailures() {
        when(properties.findByUserIdAndActiveTrue(administrator))
                .thenThrow(new DataAccessResourceFailureException("private database details"));
        assertFailure();
        verifyNoInteractions(reports, exporter);
    }

    @Test
    void shouldHideMongoFailures() {
        var property = Property.builder().id(UUID.randomUUID()).build();
        when(properties.findByUserIdAndActiveTrue(administrator)).thenReturn(List.of(property));
        when(reports.findByIdAndRelationalPropertyIdIn(anyString(), anyCollection()))
                .thenThrow(new DataAccessResourceFailureException("private query details"));
        assertFailure();
        verifyNoInteractions(exporter);
    }

    @Test
    void shouldHideWorkbookIoFailures() throws Exception {
        var report = accessibleReport();
        when(exporter.export(report)).thenThrow(new IOException("private cell contents"));
        assertFailure();
    }

    @Test
    void shouldHideWorkbookRuntimeFailures() throws Exception {
        var report = accessibleReport();
        when(exporter.export(report)).thenThrow(new IllegalArgumentException("cell limit exceeded"));
        assertFailure();
    }

    private HerdReport accessibleReport() {
        var property = Property.builder().id(UUID.randomUUID()).build();
        var report = HerdReport.builder().id("report").build();
        when(properties.findByUserIdAndActiveTrue(administrator)).thenReturn(List.of(property));
        when(reports.findByIdAndRelationalPropertyIdIn("report", List.of(property.getId().toString())))
                .thenReturn(Optional.of(report));
        return report;
    }

    private void assertFailure() {
        assertThatThrownBy(() -> service.export("report", administrator))
                .isInstanceOf(ArgosInternalServerException.class)
                .hasMessage("Não foi possível exportar o relatório");
    }
}
