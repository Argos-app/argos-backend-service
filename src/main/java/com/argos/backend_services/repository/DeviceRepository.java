package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {
    Optional<Device> findByIdAndUser_Id(UUID id, UUID userId);
    Optional<Device> findByPushToken(String pushToken);
    boolean existsByPushTokenAndIdNot(String pushToken, UUID id);
}
