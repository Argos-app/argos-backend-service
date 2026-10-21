package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.DeviceSyncIn;
import com.argos.backend_services.dto.out.DeviceSyncOut;
import com.argos.backend_services.entity.Device;
import com.argos.backend_services.exception.DeviceConflictException;
import com.argos.backend_services.exception.NotFoundException;
import com.argos.backend_services.repository.DeviceRepository;
import com.argos.backend_services.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(timeout = 30)
    public DeviceSyncOut sync(DeviceSyncIn input, UUID userId) throws DeviceConflictException {
        var device = input.deviceId() == null
                ? findOrCreate(input.pushToken(), userId)
                : find(input.deviceId(), userId);
        if (input.deviceId() != null && !Objects.equals(device.getPushToken(), input.pushToken())
                && deviceRepository.existsByPushTokenAndIdNot(input.pushToken(), device.getId())) {
            throw new DeviceConflictException("Push token já está associado a outro dispositivo");
        }

        var now = LocalDateTime.now(clock);
        if (device.getId() == null) {
            device.setCreatedAt(now);
        }
        device.setPushToken(input.pushToken());
        device.setPlatform(input.platform());
        device.setModel(input.model());
        device.setLastSync(now);
        device.setUpdatedAt(now);
        device.setActive(true);

        return new DeviceSyncOut(deviceRepository.saveAndFlush(device).getId());
    }

    @Transactional(timeout = 30)
    public void deactivate(UUID deviceId, UUID userId) {
        var device = find(deviceId, userId);
        if (!device.isActive() && device.getPushToken() == null) {
            return;
        }
        device.setActive(false);
        device.setPushToken(null);
        device.setUpdatedAt(LocalDateTime.now(clock));
    }

    private Device findOrCreate(String pushToken, UUID userId) throws DeviceConflictException {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        var device = deviceRepository.findByPushToken(pushToken).orElseGet(() -> {
            var created = new Device();
            created.setUser(user);
            return created;
        });
        if (!device.getUser().getId().equals(userId)) {
            throw new DeviceConflictException("Push token já está associado a outro usuário");
        }
        return device;
    }

    private Device find(UUID deviceId, UUID userId) {
        return deviceRepository.findByIdAndUser_Id(deviceId, userId)
                .orElseThrow(() -> new NotFoundException("Dispositivo não encontrado"));
    }
}
