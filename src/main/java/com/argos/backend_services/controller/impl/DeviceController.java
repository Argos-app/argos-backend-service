package com.argos.backend_services.controller.impl;

import com.argos.backend_services.controller.DeviceControllerApi;
import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.dto.in.DeviceSyncIn;
import com.argos.backend_services.dto.out.DeviceSyncOut;
import com.argos.backend_services.security.CurrentUserData;
import com.argos.backend_services.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController implements DeviceControllerApi {
    private final DeviceService deviceService;

    @Override
    public ResponseEntity<DeviceSyncOut> sync(DeviceSyncIn input, @CurrentUser CurrentUserData user) {
        return ResponseEntity.ok(deviceService.sync(input, user.id()));
    }

    @Override
    public ResponseEntity<Void> deactivate(UUID deviceId, @CurrentUser CurrentUserData user) {
        deviceService.deactivate(deviceId, user.id());
        return ResponseEntity.noContent().build();
    }
}
