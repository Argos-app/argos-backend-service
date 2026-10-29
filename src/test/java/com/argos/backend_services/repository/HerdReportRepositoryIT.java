package com.argos.backend_services.repository;

import com.argos.backend_services.entity.HerdReport;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
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
}
