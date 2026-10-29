package com.argos.backend_services.service;

import com.argos.backend_services.exception.ArgosInternalServerException;
import com.argos.backend_services.exception.NotFoundException;
import com.argos.backend_services.export.HerdReportXlsxExporter;
import com.argos.backend_services.repository.HerdReportRepository;
import com.argos.backend_services.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HerdReportExportService {
    private static final String NOT_FOUND = "Relatório não encontrado";

    private final PropertyRepository propertyRepository;
    private final HerdReportRepository reportRepository;
    private final HerdReportXlsxExporter exporter;

    public byte[] export(String reportId, UUID administratorId) {
        try {
            var propertyIds = propertyRepository.findByUserIdAndActiveTrue(administratorId).stream()
                    .map(property -> property.getId().toString()).toList();
            if (propertyIds.isEmpty()) {
                throw new NotFoundException(NOT_FOUND);
            }
            var report = reportRepository.findByIdAndRelationalPropertyIdIn(reportId, propertyIds)
                    .orElseThrow(() -> new NotFoundException(NOT_FOUND));
            return exporter.export(report);
        } catch (NotFoundException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            if (log.isErrorEnabled()) {
                log.error("herd_report_export_failed userId={} cause={}",
                        administratorId, exception.getClass().getSimpleName());
            }
            var failure = new ArgosInternalServerException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "HERD_REPORT_EXPORT_FAILED", "Não foi possível exportar o relatório");
            failure.initCause(exception);
            throw failure;
        }
    }
}
