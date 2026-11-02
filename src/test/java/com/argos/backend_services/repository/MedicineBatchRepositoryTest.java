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

    private Supplier batchSupplier;

    @BeforeEach
    void setUp() {
        batchSupplier = Supplier.builder().companyName("Fornecedor do lote")
                .cnpj("12345678000190").build();
        var catalogSupplier = Supplier.builder().companyName("Fornecedor do catálogo")
                .cnpj("12345678000191").build();
        var category = TherapeuticCategory.builder().name("Vacinas").build();
        em.persist(batchSupplier);
        em.persist(catalogSupplier);
        em.persist(category);

        var medicine = Medicine.builder()
                .supplierId(catalogSupplier)
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

        em.persist(batch(medicine, "LOTE-NOVO", batchSupplier));
        em.persist(batch(medicine, "LOTE-HISTORICO", null));
        em.flush();
        em.clear();
    }

    @Test
    void shouldFetchOptionalSupplierAssociatedWithBatch() {
        var page = medicineBatches.findByStatus(MedicineBatch.Status.DISPONIVEL, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        var newBatch = page.getContent().stream()
                .filter(batch -> batch.getBatchNumber().equals("LOTE-NOVO"))
                .findFirst().orElseThrow();
        var historicalBatch = page.getContent().stream()
                .filter(batch -> batch.getBatchNumber().equals("LOTE-HISTORICO"))
                .findFirst().orElseThrow();

        assertThat(newBatch.getSupplierId().getCompanyName()).isEqualTo("Fornecedor do lote");
        assertThat(historicalBatch.getSupplierId()).isNull();
        assertThat(newBatch.getMedicineId().getSupplierId().getCompanyName()).isEqualTo("Fornecedor do catálogo");
    }

    private MedicineBatch batch(Medicine medicine, String number, Supplier supplier) {
        return MedicineBatch.builder()
                .medicineId(medicine)
                .supplierId(supplier)
                .batchNumber(number)
                .manufactureDate(LocalDate.of(2026, 1, 1))
                .expiryDate(LocalDate.of(2028, 1, 1))
                .deliveryDate(LocalDate.of(2026, 1, 2))
                .initialQuantity(new BigDecimal("10.000"))
                .status(MedicineBatch.Status.DISPONIVEL)
                .build();
    }
}
