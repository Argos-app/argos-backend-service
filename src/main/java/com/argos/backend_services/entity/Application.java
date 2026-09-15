package com.argos.backend_services.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "application")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_batch_id", nullable = false)
    private MedicineBatch medicineBatchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser userId;

    @Column
    private LocalDateTime applicationDate;

    @Column
    private LocalDateTime applicationTime;

    @Column
    private BigDecimal dose;

    @Column(length = 10)
    private String unit;

    @Column(length = 20)
    private String route;

    @Column
    private Integer headCountApplied;

    @Column
    private LocalDateTime meatWithdrawalEnd;

    @Column(length = 120)
    private String goal;

    @Column
    private String notes;

    @Column
    private LocalDateTime syncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        syncedAt = LocalDateTime.now();
    }
}
