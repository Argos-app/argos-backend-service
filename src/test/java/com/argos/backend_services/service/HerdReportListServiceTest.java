package com.argos.backend_services.service;

import com.argos.backend_services.dto.out.HerdReportListItemOut;
import com.argos.backend_services.entity.HerdReport;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.exception.ArgosInternalServerException;
import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.export.HerdReportFixture;
import com.argos.backend_services.repository.HerdReportRepository;
import com.argos.backend_services.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HerdReportListServiceTest {
    @Mock PropertyRepository properties;
    @Mock HerdReportRepository reports;
    @InjectMocks ReportHerdService service;
    private final UUID administratorId = UUID.randomUUID();
    private final Property firstFarm = Property.builder().id(UUID.randomUUID()).build();
    private final Property secondFarm = Property.builder().id(UUID.randomUUID()).build();

    @Test
    void shouldListOnlyMetadataFromAllActiveOwnedFarmsWithPaginationAndStableSort() {
        var ids = List.of(firstFarm.getId().toString(), secondFarm.getId().toString());
        var report = HerdReportFixture.complete();
        when(properties.findByUserIdAndActiveTrue(administratorId)).thenReturn(List.of(firstFarm, secondFarm));
        when(reports.findByRelationalPropertyIdIn(eq(ids), any(Pageable.class)))
                .thenAnswer(call -> new PageImpl<>(List.of(report), call.getArgument(1, Pageable.class), 5));

        var result = service.list(null, 1, 2, administratorId);

        assertThat(result.data()).containsExactly(new HerdReportListItemOut("report-1", "ON_DEMAND",
                LocalDateTime.of(2026, 1, 2, 8, 30), LocalDateTime.of(2026, 1, 3, 8, 30),
                "property-1", "Fazenda São João", "Rebanho histórico", "Nelore"));
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.first()).isFalse();
        assertThat(result.last()).isFalse();
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(reports).findByRelationalPropertyIdIn(eq(ids), pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(
                Sort.by(Sort.Order.desc("reportCreatedAt"), Sort.Order.desc("id")));
    }

    @Test
    void shouldRestrictQueryToSelectedOwnedFarmAndAllowMissingHerdMetadata() {
        when(properties.findByUserIdAndActiveTrue(administratorId)).thenReturn(List.of(firstFarm, secondFarm));
        when(reports.findByRelationalPropertyIdIn(eq(List.of(secondFarm.getId().toString())), any(Pageable.class)))
                .thenAnswer(call -> new PageImpl<>(List.of(HerdReport.builder().id("legacy").build()),
                        call.getArgument(1, Pageable.class), 1));

        var result = service.list(secondFarm.getId(), 0, 6, administratorId);

        assertThat(result.data()).containsExactly(new HerdReportListItemOut("legacy", null, null, null,
                null, null, null, null));
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void shouldRejectForeignOrInactiveFarmWithoutReadingMongo() {
        when(properties.findByUserIdAndActiveTrue(administratorId)).thenReturn(List.of(firstFarm));
        assertThatThrownBy(() -> service.list(secondFarm.getId(), 0, 6, administratorId))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(reports);
    }

    @Test
    void shouldReturnAnEmptyPageWithoutReadingMongoWhenNoActiveOwnedFarmsExist() {
        when(properties.findByUserIdAndActiveTrue(administratorId)).thenReturn(List.of());
        var result = service.list(null, 0, 6, administratorId);
        assertThat(result.data()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(6);
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue();
        verifyNoInteractions(reports);
    }

    @ParameterizedTest
    @CsvSource({"-1,6", "0,0", "0,-1", "0,101"})
    void shouldRejectInvalidPaginationBeforeReadingDatabases(int page, int size) {
        assertThatThrownBy(() -> service.list(null, page, size, administratorId))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(properties, reports);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void shouldAcceptPageSizeBoundariesAndPreserveEmptyPageMetadata(int size) {
        when(properties.findByUserIdAndActiveTrue(administratorId)).thenReturn(List.of(firstFarm));
        when(reports.findByRelationalPropertyIdIn(anyCollection(), any(Pageable.class)))
                .thenAnswer(call -> Page.empty(call.getArgument(1, Pageable.class)));
        var result = service.list(null, 3, size, administratorId);
        assertThat(result.data()).isEmpty();
        assertThat(result.page()).isEqualTo(3);
        assertThat(result.size()).isEqualTo(size);
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    @Test
    void shouldHidePropertyDatabaseFailures() {
        when(properties.findByUserIdAndActiveTrue(administratorId))
                .thenThrow(new DataAccessResourceFailureException("private database credentials"));
        assertThatThrownBy(() -> service.list(null, 0, 6, administratorId))
                .isInstanceOf(ArgosInternalServerException.class).hasMessage("Não foi possível listar os relatórios");
        verifyNoInteractions(reports);
    }

    @Test
    void shouldHideMongoFailures() {
        when(properties.findByUserIdAndActiveTrue(administratorId)).thenReturn(List.of(firstFarm));
        var failure = new DataAccessResourceFailureException("private query details");
        when(reports.findByRelationalPropertyIdIn(anyCollection(), any(Pageable.class))).thenThrow(failure);
        assertThatThrownBy(() -> service.list(null, 0, 6, administratorId))
                .isInstanceOf(ArgosInternalServerException.class).hasMessage("Não foi possível listar os relatórios")
                .hasCause(failure);
    }
}
