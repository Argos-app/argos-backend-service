package com.argos.backend_services.entity;

import com.argos.backend_services.entity.enums.DevicePlatform;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "device")
@Getter
@Setter
@NoArgsConstructor
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "push_token", unique = true, columnDefinition = "text")
    private String pushToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DevicePlatform platform;

    @Column(length = 60)
    private String model;

    @Column(name = "last_sync")
    private LocalDateTime lastSync;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastSync = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        lastSync = LocalDateTime.now();
    }
}
