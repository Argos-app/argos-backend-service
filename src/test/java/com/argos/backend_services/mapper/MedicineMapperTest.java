package com.argos.backend_services.mapper;

import com.argos.backend_services.entity.Medicine;
import com.argos.backend_services.entity.MedicineBatch;
import com.argos.backend_services.entity.Supplier;
import com.argos.backend_services.entity.TherapeuticCategory;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDate;

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

    @Test
    void toListItemOutMapsExpandedBatchDetailsAndAllowsHistoricalSupplierToBeNull() {
        var supplier = Supplier.builder().companyName("Fornecedor do lote").build();
        var medicine = Medicine.builder()
                .brandName("Ivermectina")
                .dose(new BigDecimal("1.500"))
                .unit("ml")
                .meatWithdrawalDays(28)
                .activeIngredient("Ivermectina")
                .therapeuticCategoryId(TherapeuticCategory.builder().name("Antiparasitário").build())
                .build();
        var batch = MedicineBatch.builder()
                .id(UUID.randomUUID())
                .medicineId(medicine)
                .supplierId(supplier)
                .manufactureDate(LocalDate.of(2026, 1, 10))
                .expiryDate(LocalDate.of(2028, 1, 10))
                .batchNumber("LOTE-2026-001")
                .build();

        var result = mapper.toListItemOut(batch, new BigDecimal("12.500"));

        assertThat(result.medicineBatchId()).isEqualTo(batch.getId());
        assertThat(result.medicineName()).isEqualTo("Ivermectina");
        assertThat(result.supplier()).isEqualTo("Fornecedor do lote");
        assertThat(result.manufactureDate()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(result.expiryDate()).isEqualTo(LocalDate.of(2028, 1, 10));
        assertThat(result.quantity()).isEqualByComparingTo("12.500");
        assertThat(result.dose()).isEqualByComparingTo("1.500");
        assertThat(result.doseUnit()).isEqualTo("ml");
        assertThat(result.withdrawalDays()).isEqualTo(28);
        assertThat(result.batch()).isEqualTo("LOTE-2026-001");
        assertThat(result.activeIngredient()).isEqualTo("Ivermectina");
        assertThat(result.therapeuticCategory()).isEqualTo("Antiparasitário");

        batch.setSupplierId(null);
        assertThat(mapper.toListItemOut(batch, BigDecimal.ZERO).supplier()).isNull();
    }
}
