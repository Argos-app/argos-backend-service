package com.argos.backend_services.export;

import com.argos.backend_services.entity.HerdReport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class HerdReportFixture {
    private HerdReportFixture() { }

    public static HerdReport complete() {
        var date = LocalDate.of(2026, 1, 2);
        var timestamp = LocalDateTime.of(2026, 1, 2, 8, 30);
        var individual = new HerdReport.ExceptionApplication();
        individual.setApplicationAnimalId("individual-1");
        individual.setApplicationId("application-1");
        individual.setLotId("application-lot");
        individual.setLotCode("L-ANTIGO");
        individual.setApplicationDate(date);
        individual.setApplicationTime(timestamp);
        individual.setApplicatorUserId("user-1");
        individual.setMedicineBatchId("batch-1");
        individual.setDoseGiven(new BigDecimal("1.25"));
        individual.setUnit("ML");
        individual.setRoute("IM");
        individual.setWeightAtTime(new BigDecimal("250.5"));
        individual.setGoal("Tratamento");
        individual.setNotes("Observação individual");
        individual.setMeatWithdrawalEnd(date.plusDays(7));
        individual.setReleasedForSlaughter(false);
        var animal = HerdReport.Animal.builder().id("animal-1").earTag("000123").category("BOI")
                .birthDate(date.minusYears(1)).currentWeight(new BigDecimal("260.75"))
                .exceptionDate(date).exceptionReason("Febre").status("ATIVO").fitForSlaughter(false)
                .slaughterReleaseDate(date.plusDays(7)).exceptionEndDate(date.plusDays(1))
                .originLotId("origin-lot").lotId("animal-lot")
                .exceptionApplications(List.of(individual))
                .occurrences(List.of(HerdReport.Occurrence.builder().id("occurrence-1")
                        .recordedByUserId("user-2").type("CLINICA").date(date).description("Descrição")
                        .diagnosis("Diagnóstico").resolved(false).build()))
                .lotHistory(List.of(HerdReport.LotHistory.builder().id("movement-1").lotId("history-lot")
                        .entryDate(date.minusDays(1)).exitDate(date).reason("Mudança").build())).build();
        var application = HerdReport.Application.builder().id("application-1").date(date)
                .time(LocalTime.of(8, 30)).applicatorUserId("user-1").medicineBatchId("batch-1")
                .dose(new BigDecimal("2.5")).unit("ML").route("IM").headCountApplied(10)
                .goal("Prevenção").notes("=SUM(A1:A2)").meatWithdrawalEnd(date.plusDays(7)).syncedAt(timestamp).build();
        var lot = HerdReport.Lot.builder().id("lot-1").parentLotId("parent-lot").code("L-01")
                .category("Engorda").exceptionLot(true).exceptionReason("Observação").headCount(12)
                .openDate(date.minusMonths(1)).closeDate(date.plusMonths(1)).fitForSlaughter(false)
                .slaughterReleaseDate(date.plusDays(7)).applications(List.of(application)).animal(List.of(animal)).build();
        return HerdReport.builder().id("report-1").reportCreatedAt(timestamp).lastUpdatedAt(timestamp.plusDays(1))
                .reportType("ON_DEMAND").relationalPropertyId("property-1").propertyName("Fazenda São João")
                .herd(HerdReport.Herd.builder().id("herd-1").name("Rebanho histórico").breed("Nelore").build())
                .lots(List.of(lot)).historySummary(HerdReport.HistorySummary.builder()
                        .totalLots(77).totalExceptionLots(2L).totalExceptionAnimals(3).totalLotApplications(4)
                        .totalExceptionApplications(5).totalOccurrences(6).openOccurrenceCount(7)
                        .firstIndividualHistoryDate(date).lastLotChangeDate(date.plusDays(1))
                        .lastExceptionApplicationDate(date.plusDays(2)).lastOccurrenceDate(date.plusDays(3))
                        .allExceptionAnimalsFitForSlaughter(false)
                        .latestExceptionAnimalSlaughterReleaseDate(date.plusDays(7)).build()).build();
    }
}
