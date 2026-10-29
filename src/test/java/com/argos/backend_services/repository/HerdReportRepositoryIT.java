package com.argos.backend_services.repository;

import com.argos.backend_services.entity.HerdReport;
import com.argos.backend_services.export.HerdReportFixture;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest(properties = "spring.data.mongodb.uri=mongodb://127.0.0.1:27017/?serverSelectionTimeoutMS=3000")
@ActiveProfiles("test")
class HerdReportRepositoryIT {
    private static final String DATABASE = "argos_herd_report_it_" + UUID.randomUUID();

    @Autowired private HerdReportRepository reports;
    @Autowired private MongoTemplate mongoTemplate;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.database", () -> DATABASE);
    }

    @AfterEach
    void cleanUpTestDatabase() {
        mongoTemplate.getDb().drop();
    }

    @Test
    void shouldSelectRequestedHistoricalDocumentAndExcludeOtherFarms() {
        String ownedFarm = UUID.randomUUID().toString();
        String foreignFarm = UUID.randomUUID().toString();
        var createdAt = LocalDateTime.of(2026, 1, 2, 8, 30);
        var herd = HerdReport.Herd.builder().id(UUID.randomUUID().toString()).build();
        var historical = reports.insert(HerdReport.builder()
                .relationalPropertyId(ownedFarm).herd(herd)
                .reportCreatedAt(createdAt).propertyName("Fazenda no histórico").build());
        var latest = reports.insert(HerdReport.builder()
                .relationalPropertyId(ownedFarm).herd(herd)
                .reportCreatedAt(createdAt.plusDays(1)).propertyName("Fazenda atual").build());
        var foreign = reports.insert(HerdReport.builder()
                .relationalPropertyId(foreignFarm).propertyName("Outra fazenda").build());

        assertThat(reports.findByIdAndRelationalPropertyIdIn(historical.getId(), List.of(ownedFarm)))
                .hasValueSatisfying(report -> {
                    assertThat(report.getId()).isEqualTo(historical.getId()).isNotEqualTo(latest.getId());
                    assertThat(report.getPropertyName()).isEqualTo("Fazenda no histórico");
                    assertThat(report.getReportCreatedAt()).isEqualTo(createdAt);
                });
        assertThat(reports.findByIdAndRelationalPropertyIdIn(latest.getId(), List.of(ownedFarm)))
                .hasValueSatisfying(report -> assertThat(report.getId()).isEqualTo(latest.getId()));
        assertThat(reports.findByIdAndRelationalPropertyIdIn(foreign.getId(), List.of(foreignFarm))).isPresent();
        assertThat(reports.findByIdAndRelationalPropertyIdIn(foreign.getId(), List.of(ownedFarm))).isEmpty();
        assertThat(reports.findByIdAndRelationalPropertyIdIn(new ObjectId().toHexString(), List.of(ownedFarm)))
                .isEmpty();
    }

    @Test
    void shouldPageOnlyMetadataAcrossAuthorizedFarmsInStableNewestFirstOrder() {
        String firstFarm = UUID.randomUUID().toString();
        String secondFarm = UUID.randomUUID().toString();
        var createdAt = LocalDateTime.of(2026, 1, 2, 8, 30);
        var oldest = insertReport("000000000000000000000001", firstFarm, createdAt.minusDays(1));
        var tied = insertReport("000000000000000000000002", secondFarm, createdAt);
        var newest = insertReport("000000000000000000000003", firstFarm, createdAt);
        insertReport("000000000000000000000004", UUID.randomUUID().toString(), createdAt.plusDays(1));
        var ids = List.of(firstFarm, secondFarm);
        var sort = Sort.by(Sort.Order.desc("reportCreatedAt"), Sort.Order.desc("id"));

        for (int page = 0; page < 3; page++) {
            var result = reports.findByRelationalPropertyIdIn(ids, PageRequest.of(page, 1, sort));
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.getNumber()).isEqualTo(page);
            assertThat(result.getContent()).extracting(HerdReport::getId)
                    .containsExactly(List.of(newest.getId(), tied.getId(), oldest.getId()).get(page));
            var metadata = result.getContent().getFirst();
            assertThat(metadata.getReportType()).isEqualTo("ON_DEMAND");
            assertThat(metadata.getReportCreatedAt()).isNotNull();
            assertThat(metadata.getLastUpdatedAt()).isNotNull();
            assertThat(metadata.getRelationalPropertyId()).isIn(firstFarm, secondFarm);
            assertThat(metadata.getPropertyName()).isEqualTo("Fazenda São João");
            assertThat(metadata.getHerd().getName()).isEqualTo("Rebanho histórico");
            assertThat(metadata.getHerd().getBreed()).isEqualTo("Nelore");
            assertThat(metadata.getHerd().getId()).isNull();
            assertThat(metadata.getLots()).isNull();
            assertThat(metadata.getHistorySummary()).isNull();
        }
        var stored = reports.findById(newest.getId()).orElseThrow();
        assertThat(stored.getLots()).hasSize(1);
        assertThat(stored.getHistorySummary()).isNotNull();
    }

    @Test
    void shouldFilterPageCountsAndHandleMissingReportsAndPages() {
        String selectedFarm = UUID.randomUUID().toString();
        var createdAt = LocalDateTime.of(2026, 1, 2, 8, 30);
        var selected = insertReport("000000000000000000000001", selectedFarm, createdAt);
        insertReport("000000000000000000000002", UUID.randomUUID().toString(), createdAt);
        var firstPage = reports.findByRelationalPropertyIdIn(List.of(selectedFarm), PageRequest.of(0, 1));
        assertThat(firstPage.getTotalElements()).isEqualTo(1);
        assertThat(firstPage.getContent()).extracting(HerdReport::getId).containsExactly(selected.getId());

        var outside = reports.findByRelationalPropertyIdIn(List.of(selectedFarm), PageRequest.of(3, 1));
        assertThat(outside.getContent()).isEmpty();
        assertThat(outside.getNumber()).isEqualTo(3);
        assertThat(outside.getTotalElements()).isEqualTo(1);
        assertThat(outside.getTotalPages()).isEqualTo(1);

        var empty = reports.findByRelationalPropertyIdIn(List.of(UUID.randomUUID().toString()), PageRequest.of(0, 6));
        assertThat(empty.getContent()).isEmpty();
        assertThat(empty.getTotalElements()).isZero();
        assertThat(empty.getTotalPages()).isZero();
        assertThat(reports.findByRelationalPropertyIdIn(List.of(), PageRequest.of(0, 6)).getContent()).isEmpty();
    }

    private HerdReport insertReport(String id, String propertyId, LocalDateTime createdAt) {
        var report = HerdReportFixture.complete();
        report.setId(id);
        report.setRelationalPropertyId(propertyId);
        report.setReportCreatedAt(createdAt);
        return reports.insert(report);
    }
}
