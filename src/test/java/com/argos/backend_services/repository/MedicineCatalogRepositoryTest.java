package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Medicine;
import com.argos.backend_services.entity.Supplier;
import com.argos.backend_services.entity.TherapeuticCategory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
class MedicineCatalogRepositoryTest {

    @Autowired private EntityManager em;
    @Autowired private MedicineRepository medicines;

    private UUID alphaId;

    @BeforeEach
    void setUp() {
        var supplier = Supplier.builder().companyName("Fornecedor teste").cnpj("12345678000190").build();
        var category = TherapeuticCategory.builder().name("Vacinas").build();
        em.persist(supplier);
        em.persist(category);

        var alpha = medicine(supplier, category, "Alfa", true);
        em.persist(alpha);
        alphaId = alpha.getId();
        em.persist(medicine(supplier, category, "Beta", true));
        em.persist(medicine(supplier, category, "Inativo", false));
        em.flush();
        em.clear();
    }

    @Test
    void shouldPageOnlyActiveMedicinesWithoutRequiringAvailableBatches() {
        var sort = Sort.by(Sort.Order.asc("brandName"), Sort.Order.asc("id"));
        var firstPage = medicines.findByActiveTrue(PageRequest.of(0, 1, sort));
        var secondPage = medicines.findByActiveTrue(PageRequest.of(1, 1, sort));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent()).extracting(Medicine::getId).containsExactly(alphaId);
        assertThat(secondPage.getContent()).extracting(Medicine::getBrandName).containsExactly("Beta");
    }

    private Medicine medicine(Supplier supplier, TherapeuticCategory category, String name, boolean active) {
        return Medicine.builder()
                .supplierId(supplier)
                .therapeuticCategoryId(category)
                .brandName(name)
                .activeIngredient("Princípio ativo")
                .mapaRegistration("MAPA-" + name)
                .indication("Indicação")
                .description("Descrição")
                .needsPrescription(false)
                .meatWithdrawalDays(0)
                .dose(BigDecimal.ONE)
                .unit("ml")
                .route("IM")
                .active(active)
                .build();
    }
}
