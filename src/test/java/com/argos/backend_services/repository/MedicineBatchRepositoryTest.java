package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Medicine;
import com.argos.backend_services.entity.MedicineBatch;
import com.argos.backend_services.entity.Supplier;
import com.argos.backend_services.entity.TherapeuticCategory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
class MedicineBatchRepositoryTest {

    @Autowired private EntityManager em;
    @Autowired private MedicineBatchRepository medicineBatches;

    @BeforeEach
    void setUp() {
        var supplier = Supplier.builder().companyName("Fornecedor do medicamento")
                .cnpj("12345678000191").build();
        var category = TherapeuticCategory.builder().name("Vacinas").build();
        em.persist(supplier);
        em.persist(category);

        var medicine = Medicine.builder()
                .supplierId(supplier)
                .therapeuticCategoryId(category)
                .brandName("Vacina")
                .activeIngredient("Ativo")
                .mapaRegistration("MAPA-123")
                .indication("Indicação")
                .description("Descrição")
                .needsPrescription(false)
                .meatWithdrawalDays(0)
                .dose(BigDecimal.ONE)
                .unit("ml")
                .route("ORAL")
                .active(true)
                .build();
        em.persist(medicine);

        em.persist(batch(medicine, "LOTE-NOVO"));
        em.persist(batch(medicine, "LOTE-HISTORICO"));
        em.flush();
        em.clear();
    }

    @Test
    void shouldFetchSupplierAssociatedWithMedicineForEveryBatch() {
        var page = medicineBatches.findByStatus(MedicineBatch.Status.DISPONIVEL, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        var newBatch = page.getContent().stream()
                .filter(batch -> batch.getBatchNumber().equals("LOTE-NOVO"))
                .findFirst().orElseThrow();
        var historicalBatch = page.getContent().stream()
                .filter(batch -> batch.getBatchNumber().equals("LOTE-HISTORICO"))
                .findFirst().orElseThrow();

        assertThat(newBatch.getMedicineId().getSupplierId().getCompanyName()).isEqualTo("Fornecedor do medicamento");
        assertThat(historicalBatch.getMedicineId().getSupplierId().getCompanyName()).isEqualTo("Fornecedor do medicamento");
    }

    private MedicineBatch batch(Medicine medicine, String number) {
        return MedicineBatch.builder()
                .medicineId(medicine)
                .batchNumber(number)
                .manufactureDate(LocalDate.of(2026, 1, 1))
                .expiryDate(LocalDate.of(2028, 1, 1))
                .deliveryDate(LocalDate.of(2026, 1, 2))
                .initialQuantity(new BigDecimal("10.000"))
                .status(MedicineBatch.Status.DISPONIVEL)
                .build();
    }
}
