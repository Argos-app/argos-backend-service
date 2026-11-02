package com.argos.backend_services.export;

import com.argos.backend_services.entity.HerdReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HerdReportXlsxExporter {
    private static final String REPORT_ID_HEADER = "ID do relatório";
    private static final String REPORT_LOT_ID_HEADER = "ID do lote no relatório";
    private static final String ANIMAL_ID_HEADER = "ID do animal";
    private final ObjectMapper objectMapper;

    public byte[] export(HerdReport report) throws IOException {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var font = workbook.createFont();
            font.setBold(true);
            var headerStyle = workbook.createCellStyle();
            headerStyle.setFont(font);
            headerStyle.setWrapText(true);

            var summary = sheet(workbook, headerStyle, "Resumo", REPORT_ID_HEADER, "Criado em", "Atualizado em",
                    "Tipo do relatório", "ID da propriedade", "Nome da propriedade", "ID do rebanho", "Nome do rebanho", "Raça");
            var lots = sheet(workbook, headerStyle, "Lotes", REPORT_ID_HEADER, "ID do lote", "ID do lote pai",
                    "Código", "Categoria", "Lote de exceção", "Motivo da exceção", "Quantidade de cabeças",
                    "Data de abertura", "Data de fechamento", "Apto para abate", "Data de liberação para abate");
            var applications = sheet(workbook, headerStyle, "Aplicações", REPORT_ID_HEADER, REPORT_LOT_ID_HEADER,
                    "ID da aplicação", "Data", "Horário", "ID do aplicador", "ID do lote de medicamento", "Dose",
                    "Unidade", "Via", "Cabeças aplicadas", "Objetivo", "Observações", "Fim da carência de carne", "Sincronizado em");
            var animals = sheet(workbook, headerStyle, "Animais", REPORT_ID_HEADER, REPORT_LOT_ID_HEADER,
                    ANIMAL_ID_HEADER, "Brinco", "Categoria", "Data de nascimento", "Peso atual", "Data da exceção",
                    "Motivo da exceção", "Status", "Apto para abate", "Data de liberação para abate",
                    "Data de fim da exceção", "ID do lote de origem", "ID do lote do animal");
            var individualApplications = sheet(workbook, headerStyle, "Aplicações individuais", REPORT_ID_HEADER,
                    REPORT_LOT_ID_HEADER, ANIMAL_ID_HEADER, "ID da aplicação individual", "ID da aplicação",
                    "ID do lote da aplicação", "Código do lote da aplicação", "Data da aplicação", "Horário da aplicação",
                    "ID do aplicador", "ID do lote de medicamento", "Dose aplicada", "Unidade", "Via", "Peso na aplicação",
                    "Objetivo", "Observações", "Fim da carência de carne", "Liberado para abate");
            var occurrences = sheet(workbook, headerStyle, "Ocorrências", REPORT_ID_HEADER, REPORT_LOT_ID_HEADER,
                    ANIMAL_ID_HEADER, "ID da ocorrência", "ID do usuário responsável", "Tipo", "Data", "Descrição", "Diagnóstico", "Resolvida");
            var history = sheet(workbook, headerStyle, "Histórico de lotes", REPORT_ID_HEADER, REPORT_LOT_ID_HEADER,
                    ANIMAL_ID_HEADER, "ID da movimentação", "ID do lote da movimentação", "Data de entrada", "Data de saída", "Motivo");
            var indicators = sheet(workbook, headerStyle, "Indicadores", REPORT_ID_HEADER, "Total de lotes",
                    "Total de lotes de exceção", "Total de animais de exceção", "Total de aplicações em lotes",
                    "Total de aplicações individuais", "Total de ocorrências", "Ocorrências abertas",
                    "Primeira data de histórico individual", "Última mudança de lote", "Última aplicação individual",
                    "Última ocorrência", "Todos os animais de exceção aptos para abate", "Última data de liberação para abate");

            var herd = report.getHerd();
            row(summary, report.getId(), report.getReportCreatedAt(), report.getLastUpdatedAt(), report.getReportType(),
                    report.getRelationalPropertyId(), report.getPropertyName(), herd == null ? null : herd.getId(),
                    herd == null ? null : herd.getName(), herd == null ? null : herd.getBreed());
            for (var lot : elements(report.getLots())) {
                row(lots, report.getId(), lot.getId(), lot.getParentLotId(), lot.getCode(), lot.getCategory(),
                        lot.getExceptionLot(), lot.getExceptionReason(), lot.getHeadCount(), lot.getOpenDate(),
                        lot.getCloseDate(), lot.getFitForSlaughter(), lot.getSlaughterReleaseDate());
                for (var application : elements(lot.getApplications())) {
                    row(applications, report.getId(), lot.getId(), application.getId(), application.getDate(),
                            application.getTime(), application.getApplicatorUserId(), application.getMedicineBatchId(),
                            application.getDose(), application.getUnit(), application.getRoute(), application.getHeadCountApplied(),
                            application.getGoal(), application.getNotes(), application.getMeatWithdrawalEnd(), application.getSyncedAt());
                }
                for (var animal : elements(lot.getAnimal())) {
                    row(animals, report.getId(), lot.getId(), animal.getId(), animal.getEarTag(), animal.getCategory(),
                            animal.getBirthDate(), animal.getCurrentWeight(), animal.getExceptionDate(), animal.getExceptionReason(),
                            animal.getStatus(), animal.getFitForSlaughter(), animal.getSlaughterReleaseDate(),
                            animal.getExceptionEndDate(), animal.getOriginLotId(), animal.getLotId());
                    for (var application : elements(animal.getExceptionApplications())) {
                        row(individualApplications, report.getId(), lot.getId(), animal.getId(), application.getApplicationAnimalId(),
                                application.getApplicationId(), application.getLotId(), application.getLotCode(),
                                application.getApplicationDate(), application.getApplicationTime(), application.getApplicatorUserId(),
                                application.getMedicineBatchId(), application.getDoseGiven(), application.getUnit(), application.getRoute(),
                                application.getWeightAtTime(), application.getGoal(), application.getNotes(),
                                application.getMeatWithdrawalEnd(), application.getReleasedForSlaughter());
                    }
                    for (var occurrence : elements(animal.getOccurrences())) {
                        row(occurrences, report.getId(), lot.getId(), animal.getId(), occurrence.getId(),
                                occurrence.getRecordedByUserId(), occurrence.getType(), occurrence.getDate(),
                                occurrence.getDescription(), occurrence.getDiagnosis(), occurrence.getResolved());
                    }
                    for (var movement : elements(animal.getLotHistory())) {
                        row(history, report.getId(), lot.getId(), animal.getId(), movement.getId(), movement.getLotId(),
                                movement.getEntryDate(), movement.getExitDate(), movement.getReason());
                    }
                }
            }
            var stats = report.getHistorySummary();
            if (stats != null) {
                row(indicators, report.getId(), stats.getTotalLots(), stats.getTotalExceptionLots(), stats.getTotalExceptionAnimals(),
                        stats.getTotalLotApplications(), stats.getTotalExceptionApplications(), stats.getTotalOccurrences(),
                        stats.getOpenOccurrenceCount(), stats.getFirstIndividualHistoryDate(), stats.getLastLotChangeDate(),
                        stats.getLastExceptionApplicationDate(), stats.getLastOccurrenceDate(),
                        stats.getAllExceptionAnimalsFitForSlaughter(), stats.getLatestExceptionAnimalSlaughterReleaseDate());
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private Sheet sheet(XSSFWorkbook workbook, CellStyle headerStyle, String name, String... headers) {
        var sheet = workbook.createSheet(name);
        var header = sheet.createRow(0);
        header.setHeightInPoints(45);
        for (int column = 0; column < headers.length; column++) {
            var cell = header.createCell(column);
            cell.setCellValue(headers[column]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(column, 28 * 256);
        }
        sheet.createFreezePane(0, 1);
        return sheet;
    }

    private void row(Sheet sheet, Object... values) throws IOException {
        var row = sheet.createRow(sheet.getLastRowNum() + 1);
        for (int column = 0; column < values.length; column++) {
            write(row.createCell(column), values[column]);
        }
    }

    private void write(Cell cell, Object value) throws IOException {
        switch (value) {
            case null -> {}
            case String text -> cell.setCellValue(text);
            case Boolean flag -> cell.setCellValue(flag);
            case Number number -> {
                var decimal = new BigDecimal(number.toString());
                double numeric = decimal.doubleValue();
                // Excel keeps only 15 significant digits and cannot represent subnormal doubles.
                if (decimal.stripTrailingZeros().precision() <= 15 && Double.isFinite(numeric)
                        && (decimal.signum() == 0 || Math.abs(numeric) >= Double.MIN_NORMAL)
                        && BigDecimal.valueOf(numeric).compareTo(decimal) == 0) {
                    cell.setCellValue(numeric);
                } else {
                    cell.setCellValue(number.toString());
                }
            }
            case Date date -> cell.setCellValue(date.toInstant().toString());
            case TemporalAccessor temporal -> cell.setCellValue(temporal.toString());
            default -> cell.setCellValue(objectMapper.writeValueAsString(value));
        }
    }

    private <T> List<T> elements(List<T> values) {
        return values == null ? List.of() : values;
    }
}
