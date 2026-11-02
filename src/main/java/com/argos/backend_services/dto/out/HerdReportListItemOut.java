package com.argos.backend_services.dto.out;

import com.argos.backend_services.entity.HerdReport;

import java.time.LocalDateTime;

public record HerdReportListItemOut(
        String id,
        String reportType,
        LocalDateTime reportCreatedAt,
        LocalDateTime lastUpdatedAt,
        String propertyId,
        String propertyName,
        String herdName,
        String herdBreed
) {
    public static HerdReportListItemOut from(HerdReport report) {
        var herd = report.getHerd();
        return new HerdReportListItemOut(report.getId(), report.getReportType(), report.getReportCreatedAt(),
                report.getLastUpdatedAt(), report.getRelationalPropertyId(), report.getPropertyName(),
                herd == null ? null : herd.getName(), herd == null ? null : herd.getBreed());
    }
}
