package com.argos.backend_services.mapper;

import com.argos.backend_services.entity.Medicine;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MedicineMapperTest {

    private final MedicineMapper mapper = Mappers.getMapper(MedicineMapper.class);

    @Test
    void shouldMapCatalogItemAndExposeMapaRegistrationAsProductRegistration() {
        var medicineId = UUID.randomUUID();
        var medicine = Medicine.builder()
                .id(medicineId)
                .brandName("Vacina A")
                .mapaRegistration("MAPA-123")
                .build();

        var result = mapper.toCatalogItemOut(medicine);

        assertThat(result.medicineId()).isEqualTo(medicineId);
        assertThat(result.medicineName()).isEqualTo("Vacina A");
        assertThat(result.productRegistration()).isEqualTo("MAPA-123");
    }
}
