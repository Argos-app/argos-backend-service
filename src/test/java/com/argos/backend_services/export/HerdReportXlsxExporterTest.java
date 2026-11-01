package com.argos.backend_services.export;

import com.argos.backend_services.entity.HerdReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class HerdReportXlsxExporterTest {
    private final HerdReportXlsxExporter exporter = new HerdReportXlsxExporter(
            new ObjectMapper().registerModule(new JavaTimeModule()));

    @Test
    void shouldPreserveEveryFieldAndRelationshipInEightPortugueseSheets() throws Exception {
        byte[] bytes = exporter.export(HerdReportFixture.complete());
        Files.write(Path.of("target/herd-report-example.xlsx"), bytes);
        try (var book = open(bytes)) {
            assertThat(book.getNumberOfSheets()).isEqualTo(8);
            assertThat(java.util.stream.IntStream.range(0, 8).mapToObj(book::getSheetName).toList())
                    .containsExactly("Resumo", "Lotes", "Aplicações", "Animais", "Aplicações individuais",
                            "Ocorrências", "Histórico de lotes", "Indicadores");
            assertRow(book.getSheetAt(0), "report-1", "2026-01-02T08:30", "2026-01-03T08:30", "ON_DEMAND",
                    "property-1", "Fazenda São João", "herd-1", "Rebanho histórico", "Nelore");
            assertRow(book.getSheetAt(1), "report-1", "lot-1", "parent-lot", "L-01", "Engorda", true,
                    "Observação", 12.0, "2025-12-02", "2026-02-02", false, "2026-01-09");
            assertRow(book.getSheetAt(2), "report-1", "lot-1", "application-1", "2026-01-02", "08:30",
                    "user-1", "batch-1", 2.5, "ML", "IM", 10.0, "Prevenção", "=SUM(A1:A2)", "2026-01-09", "2026-01-02T08:30");
            assertRow(book.getSheetAt(3), "report-1", "lot-1", "animal-1", "000123", "BOI", "2025-01-02",
                    260.75, "2026-01-02", "Febre", "ATIVO", false, "2026-01-09", "2026-01-03", "origin-lot", "animal-lot");
            assertRow(book.getSheetAt(4), "report-1", "lot-1", "animal-1", "individual-1", "application-1",
                    "application-lot", "L-ANTIGO", "2026-01-02", "2026-01-02T08:30", "user-1", "batch-1",
                    1.25, "ML", "IM", 250.5, "Tratamento", "Observação individual", "2026-01-09", false);
            assertRow(book.getSheetAt(5), "report-1", "lot-1", "animal-1", "occurrence-1", "user-2",
                    "CLINICA", "2026-01-02", "Descrição", "Diagnóstico", false);
            assertRow(book.getSheetAt(6), "report-1", "lot-1", "animal-1", "movement-1", "history-lot",
                    "2026-01-01", "2026-01-02", "Mudança");
            assertRow(book.getSheetAt(7), "report-1", 77.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0,
                    "2026-01-02", "2026-01-03", "2026-01-04", "2026-01-05", false, "2026-01-09");
            for (var sheet : book) {
                assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
                assertThat(book.getFontAt(sheet.getRow(0).getCell(0).getCellStyle().getFontIndex()).getBold()).isTrue();
                assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("ID do relatório");
                assertThat(sheet.getColumnWidth(0)).isEqualTo(28 * 256);
            }
        }
    }

    @Test
    void shouldKeepHeadersAndBlankCellsForMissingObjectsAndCollections() throws Exception {
        for (var lots : java.util.Arrays.asList(null, List.<HerdReport.Lot>of())) {
            var report = HerdReport.builder().lots(lots).build();
            try (var book = open(exporter.export(report))) {
                assertThat(book.getNumberOfSheets()).isEqualTo(8);
                for (int i = 0; i < 8; i++) {
                    assertThat((Object) book.getSheetAt(i).getRow(0)).isNotNull();
                    assertThat(book.getSheetAt(i).getPhysicalNumberOfRows()).isEqualTo(i == 0 ? 2 : 1);
                }
                for (var cell : book.getSheetAt(0).getRow(1)) {
                    assertThat(cell.getCellType()).isEqualTo(CellType.BLANK);
                }
            }
        }
    }

    @Test
    void shouldHandleEmptyNestedCollectionsAndNullScalarValues() throws Exception {
        var report = HerdReportFixture.complete();
        report.setHerd(HerdReport.Herd.builder().build());
        report.setHistorySummary(HerdReport.HistorySummary.builder().build());
        report.setLots(List.of(HerdReport.Lot.builder().animal(List.of(HerdReport.Animal.builder().build())).build(),
                HerdReport.Lot.builder().applications(List.of()).animal(List.of()).build()));
        try (var book = open(exporter.export(report))) {
            assertThat(book.getSheet("Lotes").getLastRowNum()).isEqualTo(2);
            assertThat(book.getSheet("Animais").getLastRowNum()).isEqualTo(1);
            assertThat(book.getSheet("Aplicações").getLastRowNum()).isZero();
            assertThat(book.getSheet("Ocorrências").getLastRowNum()).isZero();
            assertThat(book.getSheet("Indicadores").getRow(1).getCell(1).getCellType()).isEqualTo(CellType.BLANK);
        }
    }

    @Test
    void shouldPreserveLegacyValuesAndNumericPrecisionWithoutCreatingFormulas() throws Exception {
        var report = HerdReportFixture.complete();
        var lot = report.getLots().getFirst();
        var application = lot.getApplications().getFirst();
        lot.setOpenDate(Date.from(Instant.parse("2020-01-01T03:00:00Z")));
        lot.setCloseDate("2020-01-02");
        lot.setSlaughterReleaseDate(Map.of("original", List.of("value", "ação")));
        application.setDate(Map.of("legacy", "date"));
        application.setTime(true);
        application.setDose(new BigDecimal("1234567890123456.78"));
        application.setMeatWithdrawalEnd("+cmd|'test'");
        try (var book = open(exporter.export(report))) {
            assertThat(book.getSheet("Lotes").getRow(1).getCell(8).getStringCellValue()).isEqualTo("2020-01-01T03:00:00Z");
            assertThat(book.getSheet("Lotes").getRow(1).getCell(9).getStringCellValue()).isEqualTo("2020-01-02");
            assertThat(book.getSheet("Lotes").getRow(1).getCell(11).getStringCellValue())
                    .isEqualTo("{\"original\":[\"value\",\"ação\"]}");
            assertThat(book.getSheet("Aplicações").getRow(1).getCell(3).getStringCellValue()).isEqualTo("{\"legacy\":\"date\"}");
            assertThat(book.getSheet("Aplicações").getRow(1).getCell(4).getBooleanCellValue()).isTrue();
            assertThat(book.getSheet("Aplicações").getRow(1).getCell(7).getStringCellValue()).isEqualTo("1234567890123456.78");
            assertThat(book.getSheet("Aplicações").getRow(1).getCell(14).getCellType()).isEqualTo(CellType.STRING);
        }
        for (var number : List.of(BigDecimal.ZERO, new BigDecimal("1E400"), new BigDecimal("1E-400"),
                new BigDecimal("1E-310"), new BigDecimal("999999999999999"))) {
            application.setDose(number);
            try (var book = open(exporter.export(report))) {
                var cell = book.getSheet("Aplicações").getRow(1).getCell(7);
                if (number.signum() == 0 || number.compareTo(new BigDecimal("999999999999999")) == 0) {
                    assertThat(cell.getNumericCellValue()).isEqualTo(number.doubleValue());
                } else {
                    assertThat(cell.getStringCellValue()).isEqualTo(number.toString());
                }
            }
        }
    }

    @Test
    void shouldRejectExcelCellOverflowInsteadOfTruncatingData() {
        var report = HerdReportFixture.complete();
        report.setPropertyName("x".repeat(32768));
        assertThatThrownBy(() -> exporter.export(report)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldExportRepresentativeReportWithoutDeduplicatingRecords() throws Exception {
        var report = HerdReportFixture.complete();
        report.setLots(Collections.nCopies(1000, report.getLots().getFirst()));
        long heapBefore = java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        long start = System.nanoTime();
        byte[] bytes = exporter.export(report);
        long heapAfter = java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        try (var book = open(bytes)) {
            for (int i = 1; i <= 6; i++) {
                assertThat(book.getSheetAt(i).getLastRowNum()).isEqualTo(1000);
            }
        }
        org.slf4j.LoggerFactory.getLogger(getClass()).info(
                "representative_export lots=1000 bytes={} durationMs={} heapBeforeBytes={} heapAfterBytes={}",
                bytes.length, (System.nanoTime() - start) / 1_000_000, heapBefore, heapAfter);
    }

    private XSSFWorkbook open(byte[] bytes) throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    private void assertRow(Sheet sheet, Object... expected) {
        var row = sheet.getRow(1);
        assertThat((int) row.getLastCellNum()).isEqualTo(expected.length);
        assertThat((int) sheet.getRow(0).getLastCellNum()).isEqualTo(expected.length);
        for (int i = 0; i < expected.length; i++) {
            var cell = row.getCell(i);
            if (expected[i] instanceof Double numeric) {
                assertThat(cell.getNumericCellValue()).as("%s column %s", sheet.getSheetName(), i).isEqualTo(numeric);
            } else if (expected[i] instanceof Boolean flag) {
                assertThat(cell.getBooleanCellValue()).isEqualTo(flag);
            } else {
                assertThat(cell.getCellType()).isEqualTo(CellType.STRING);
                assertThat(cell.getStringCellValue()).as("%s column %s", sheet.getSheetName(), i).isEqualTo(expected[i]);
            }
        }
    }
}
