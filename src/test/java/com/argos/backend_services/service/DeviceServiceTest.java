package com.argos.backend_services.service;

import com.argos.backend_services.dto.in.DeviceSyncIn;
import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Device;
import com.argos.backend_services.entity.enums.DevicePlatform;
import com.argos.backend_services.exception.DeviceConflictException;
import com.argos.backend_services.exception.NotFoundException;
import com.argos.backend_services.repository.DeviceRepository;
import com.argos.backend_services.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {
    @Mock private DeviceRepository devices;
    @Mock private UserRepository users;
    private DeviceService service;
    private AppUser user;

    @BeforeEach
    void setUp() {
        service = new DeviceService(devices, users);
        user = AppUser.builder().id(UUID.randomUUID()).build();
    }

    @Test
    void shouldCreateDeviceAndReturnGeneratedId() throws DeviceConflictException {
        authenticate();
        UUID generatedId = UUID.randomUUID();
        when(devices.save(any(Device.class))).thenAnswer(invocation -> {
            Device device = invocation.getArgument(0);
            device.setId(generatedId);
            return device;
        });

        var result = service.sync(input(null, "new-token"), user.getId());

        assertThat(result.deviceId()).isEqualTo(generatedId);
        var captor = ArgumentCaptor.forClass(Device.class);
        verify(devices).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getPushToken()).isEqualTo("new-token");
        assertThat(captor.getValue().getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(captor.getValue().getModel()).isEqualTo("Pixel");
        assertThat(captor.getValue().isActive()).isTrue();
        verify(devices, times(1)).findByPushToken("new-token");
    }

    @Test
    void shouldReuseDeviceWhenIdWasLostButTokenBelongsToSameUser() throws DeviceConflictException {
        authenticate();
        var device = device();
        when(devices.findByPushToken("token")).thenReturn(Optional.of(device));
        when(devices.save(device)).thenReturn(device);

        assertThat(service.sync(input(null, "token"), user.getId()).deviceId()).isEqualTo(device.getId());
        verify(devices).save(device);
        verify(devices, times(1)).findByPushToken("token");
    }

    @Test
    void shouldRotateTokenAndReactivateWithoutChangingIdentityOrCreationDate() throws DeviceConflictException {
        var device = device();
        device.setActive(false);
        device.setPushToken(null);
        var created = LocalDateTime.of(2026, 1, 1, 12, 0);
        device.setCreatedAt(created);
        when(devices.findByIdAndUser_Id(device.getId(), user.getId())).thenReturn(Optional.of(device));
        when(devices.save(device)).thenReturn(device);

        assertThat(service.sync(input(device.getId(), "rotated"), user.getId()).deviceId()).isEqualTo(device.getId());
        assertThat(device.getPushToken()).isEqualTo("rotated");
        assertThat(device.isActive()).isTrue();
        assertThat(device.getCreatedAt()).isEqualTo(created);
        verifyNoInteractions(users);
    }

    @Test
    void shouldAcceptUnchangedTokenWithExplicitId() throws DeviceConflictException {
        var device = device();
        when(devices.findByIdAndUser_Id(device.getId(), user.getId())).thenReturn(Optional.of(device));
        when(devices.save(device)).thenReturn(device);

        assertThat(service.sync(input(device.getId(), "token"), user.getId()).deviceId()).isEqualTo(device.getId());
        verify(devices, never()).existsByPushTokenAndIdNot(anyString(), any());
        verifyNoInteractions(users);
    }

    @Test
    void shouldRejectTokenOwnedByAnotherUserWithoutId() {
        authenticate();
        var foreign = device();
        foreign.setUser(AppUser.builder().id(UUID.randomUUID()).build());
        when(devices.findByPushToken("token")).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.sync(input(null, "token"), user.getId()))
                .isInstanceOf(DeviceConflictException.class);
        verify(devices, never()).save(any());
    }

    @Test
    void shouldRejectTokenAssignedToAnotherRecordEvenForSameUser() {
        var first = device();
        when(devices.findByIdAndUser_Id(first.getId(), user.getId())).thenReturn(Optional.of(first));
        when(devices.existsByPushTokenAndIdNot("other-token", first.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.sync(input(first.getId(), "other-token"), user.getId()))
                .isInstanceOf(DeviceConflictException.class);
        verify(devices, never()).save(any());
    }

    @Test
    void shouldNotCreateWhenExplicitIdIsMissingOrForeign() {
        assertThatThrownBy(() -> service.sync(input(UUID.randomUUID(), "token"), user.getId()))
                .isInstanceOf(NotFoundException.class);
        verify(devices, never()).save(any());
        verify(devices, never()).findByPushToken(anyString());
    }

    @Test
    void shouldRejectUnknownUser() {
        assertThatThrownBy(() -> service.sync(input(null, "token"), user.getId()))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(devices);
    }

    @Test
    void shouldNotTranslateUnrelatedUniqueConstraint() {
        assertIntegrityFailureIsPreserved(violation("23505", "other_unique_key"));
    }

    @Test
    void shouldNotTranslateCheckConstraint() {
        assertIntegrityFailureIsPreserved(violation("23514", "ck_device_active_token"));
    }

    @Test
    void shouldNotTranslateIntegrityFailureWithoutConstraintDetails() {
        assertIntegrityFailureIsPreserved(new DataIntegrityViolationException("other failure"));
    }

    @Test
    void shouldDeactivateAndReleaseToken() {
        var device = device();
        when(devices.findByIdAndUser_Id(device.getId(), user.getId())).thenReturn(Optional.of(device));

        service.deactivate(device.getId(), user.getId());

        assertThat(device.isActive()).isFalse();
        assertThat(device.getPushToken()).isNull();
        verify(devices, never()).save(any());
        verifyNoInteractions(users);
    }

    @Test
    void shouldRejectDeactivationOfMissingOrForeignDevice() {
        assertThatThrownBy(() -> service.deactivate(UUID.randomUUID(), user.getId()))
                .isInstanceOf(NotFoundException.class);
        verify(devices, never()).save(any());
    }

    @Test
    void shouldNotChangeAuditTimestampWhenAlreadyInactive() {
        var device = device();
        device.setActive(false);
        device.setPushToken(null);
        var updatedAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        device.setUpdatedAt(updatedAt);
        when(devices.findByIdAndUser_Id(device.getId(), user.getId())).thenReturn(Optional.of(device));
        service.deactivate(device.getId(), user.getId());
        assertThat(device.getUpdatedAt()).isEqualTo(updatedAt);
    }

    private void authenticate() {
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
    }

    private DataIntegrityViolationException violation(String state, String constraint) {
        return new DataIntegrityViolationException("integrity failure",
                new ConstraintViolationException("constraint failure", new SQLException("failure", state), constraint));
    }

    private void assertIntegrityFailureIsPreserved(DataIntegrityViolationException exception) {
        authenticate();
        when(devices.save(any())).thenThrow(exception);
        assertThatThrownBy(() -> service.sync(input(null, "token"), user.getId())).isSameAs(exception);
    }

    private Device device() {
        var device = new Device();
        device.setId(UUID.randomUUID());
        device.setUser(user);
        device.setPushToken("token");
        return device;
    }

    private DeviceSyncIn input(UUID id, String token) {
        return new DeviceSyncIn(id, token, DevicePlatform.ANDROID, "Pixel");
    }
}
