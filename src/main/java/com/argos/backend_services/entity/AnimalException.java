package com.argos.backend_services.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "animal_exception")
public class AnimalException {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_lot_id", nullable = false)
    private Lot originLotId;

    @Column(name = "ear_tag", nullable = false, length = 20)
    private String earTag;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "current_weight")
    private BigDecimal currentWeight;

    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    @Column(name = "exception_reason", nullable = false, length = 255)
    private String exceptionReason;

    @Column(name = "slaughter_release_date")
    private LocalDate slaughterReleaseDate;

    @Column(name = "fit_for_slaughter", nullable = false)
    private boolean fitForSlaughter = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAnimal status = StatusAnimal.ATIVO;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        exceptionDate = LocalDate.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum StatusAnimal {
        ATIVO,
        VENDIDO,
        ABATIDO,
        MORTO,
        RECUPERADO
    }
}
