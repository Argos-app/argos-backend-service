package com.argos.backend_services.mapper;

import com.argos.backend_services.dto.in.LotIn;
import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.entity.enums.HerdCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.repository.AnimalExceptionRepository;
import com.argos.backend_services.repository.ApplicationAnimalRepository;

import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;

class LotMapperTest {
    private final AnimalExceptionRepository animalExceptionRepository = mock(AnimalExceptionRepository.class);
    private final ApplicationAnimalRepository applicationAnimalRepository = mock(ApplicationAnimalRepository.class);
    private final LotMapper mapper = new LotMapper(
            animalExceptionRepository,
            applicationAnimalRepository);
    @Test void shouldMapEntityToOutput() {
        var lot = Lot.builder().lotCode("L-1").category("Engorda").openDate(LocalDate.of(2026, 1, 2)).slaughterReleaseDate(LocalDate.of(2026, 7, 1)).build();
        var result = mapper.toOut(lot);
        assertThat(result.getLotName()).isEqualTo("L-1"); assertThat(result.getOpenDate()).isEqualTo(LocalDate.of(2026, 1, 2)); assertThat(result.getReleaseSlaughterDate()).isEqualTo(LocalDate.of(2026, 7, 1));
    }
    @ParameterizedTest
    @CsvSource({"FATTENING,ENGORDA", "BREEDING,CRIA", "REARING,RECRIA"})
    void shouldMapInputToActiveEntity(HerdCategory category, String storedCategory) {
        var herd = Herd.builder().herdName("H-1").build();
        var input = LotIn.builder().lotName("L-1").herdName("H-1").category(category).headCount(10).releaseSlaughterDate(LocalDate.of(2026, 7, 1)).build();
        var result = mapper.toEntity(input, herd);
        assertThat(result.getLotCode()).isEqualTo("L-1"); assertThat(result.getCategory()).isEqualTo(storedCategory); assertThat(result.getHeadCount()).isEqualTo(10); assertThat(result.isActive()).isTrue(); assertThat(result.isExceptionLot()).isFalse(); assertThat(result.getHerdId()).isSameAs(herd);
    }
}
