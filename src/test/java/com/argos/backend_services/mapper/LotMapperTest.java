package com.argos.backend_services.mapper;

import com.argos.backend_services.dto.in.LotIn;
import com.argos.backend_services.entity.Lot;
import org.junit.jupiter.api.Test;
import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.repository.HerdRepository;
import com.argos.backend_services.repository.LotRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class LotMapperTest {
    private final HerdRepository herdRepository = mock(HerdRepository.class);
    private final LotRepository lotRepository = mock(LotRepository.class);
    private final LotMapper mapper = new LotMapper(herdRepository, lotRepository);
    @Test void shouldMapEntityToOutput() {
        var lot = Lot.builder().lotCode("L-1").category("Engorda").openDate(LocalDate.of(2026, 1, 2)).slaughterReleaseDate(LocalDate.of(2026, 7, 1)).build();
        var result = mapper.toOut(lot);
        assertThat(result.getLotName()).isEqualTo("L-1"); assertThat(result.getOpenDAte()).isEqualTo(LocalDate.of(2026, 1, 2)); assertThat(result.getReleaseSlaughterDate()).isEqualTo(LocalDate.of(2026, 7, 1));
    }
    @Test void shouldMapInputToActiveEntity() throws Exception {
        var herd = Herd.builder().herdName("H-1").build();
        var input = LotIn.builder().lotName("L-1").herdName("H-1").category("Recria").headCount(10).releaseSlaughterDate(LocalDate.of(2026, 7, 1)).build();
        when(herdRepository.findByHerdNameAndActiveTrue("H-1")).thenReturn(Optional.of(herd));
        var result = mapper.toEntity(input);
        assertThat(result.getLotCode()).isEqualTo("L-1"); assertThat(result.getCategory()).isEqualTo("Recria"); assertThat(result.getHeadCount()).isEqualTo(10); assertThat(result.isActive()).isTrue(); assertThat(result.isExceptionLot()).isFalse(); assertThat(result.getHerdId()).isSameAs(herd);
    }
}
