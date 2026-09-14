package com.argos.backend_services.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "herd")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Herd {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property propertyId;

    @Column(name = "herd_name", nullable = false, length = 60)
    private String herdName;

    @Column(nullable = false, length = 40)
    private String breed;

    @Column(nullable = false, name = "head_count")
    private Integer headCount;

    @Column(name = "source_id")
    private Integer sourceId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, updatable = false)
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
