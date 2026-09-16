package com.argos.backend_services.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "application_animal")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationAnimal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application applicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_exception_id", nullable = false)
    private AnimalException animalExceptionId;

    @Column(name = "dose_given", nullable = false)
    private BigDecimal dose_given;

    @Column(name = "weight_at_time")
    private BigDecimal weight_at_time;

    @Column(name = "meat_withdrawal_end")
    private LocalDate meatWithdrawalDate;

    @Column(name = "released_for_slaughter", nullable = false)
    private boolean releasedForSlaughter = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
