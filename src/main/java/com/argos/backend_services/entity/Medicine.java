package com.argos.backend_services.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medicine")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplierId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "therapeutic_category_id", nullable = false)
    private TherapeuticCategory therapeuticCategoryId;

    @Column(name = "brand_name", nullable = false, length = 80)
    private String brandName;

    @Column(name = "active_ingredient", nullable = false, length = 80)
    private String activeIngredient;

    @Column(name = "mapa_registration", nullable = false, length = 20)
    private String mapaRegistration;

    @Column(nullable = false)
    private String indication;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "needs_prescription", nullable = false)
    private boolean needsPrescription;

    @Column(name = "meat_withdrawal_days", nullable = false)
    private Integer meatWithdrawalDays;

    @Column(nullable = false)
    private BigDecimal dose;

    @Column(nullable = false, length = 10)
    private String unit;

    @Column(nullable = false, length = 20)
    private String route;

    @Column(name = "source_id")
    private Integer sourceId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
