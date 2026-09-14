package com.argos.backend_services.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "herd_id", nullable = false)
    private Herd herdId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_lot_id", nullable = false)
    private Lot parentId;

    @Column(name = "lot_code", length = 20, nullable = false)
    private String lotCode;

    @Column(length = 30, nullable = false)
    private String category;

    @Column(name = "is_exception_lot", nullable = false)
    private boolean isExceptionLot;

    @Column(name = "head_count", nullable = false)
    private Integer headCount;

    @Column(name = "slaughter_release_date")
    private LocalDate slaughterReleaseDate;

    @Column(name = "fit_for_slaughter", nullable = false)
    private boolean fitForSlaughter;

    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        openDate = LocalDate.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
