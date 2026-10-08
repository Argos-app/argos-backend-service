package com.argos.backend_services.controller;

import com.argos.backend_services.dto.in.CreateUserIn;
import com.argos.backend_services.dto.in.UpdateUserAccessIn;
import com.argos.backend_services.dto.in.UpdateUserIn;
import com.argos.backend_services.dto.in.UpdateUserStatusIn;
import com.argos.backend_services.dto.out.AdminUserOptionOut;
import com.argos.backend_services.dto.out.CreateUserOut;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.dto.out.UserAccessOut;
import com.argos.backend_services.dto.out.UserCreationOptionsOut;
import com.argos.backend_services.dto.out.UserListItemOut;
import com.argos.backend_services.dto.out.UserOptionOut;
import com.argos.backend_services.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController implements UserControllerApi {
    private final UserService userService;

    @Override
    @GetMapping("/options")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserCreationOptionsOut> getCreationOptions() {
        return ResponseEntity.ok(userService.getCreationOptions());
    }

    @Override
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<UserOptionOut>> listPermissions() {
        return ResponseEntity.ok(userService.getActivePermissions());
    }

    @Override
    @GetMapping("/accesses")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PageOut<UserAccessOut>> listAccesses(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.listUserAccesses(page, size));
    }

    @Override
    @PatchMapping("/{userId}/access")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> updateAccess(@PathVariable UUID userId, @Valid @RequestBody UpdateUserAccessIn data) {
        userService.updateAccess(userId, data);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID userId, @Valid @RequestBody UpdateUserStatusIn data) {
        userService.updateStatus(userId, data);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/admins")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PageOut<AdminUserOptionOut>> listAdministrators(@RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "6") int size) {
        return ResponseEntity.ok(userService.listAdministrators(page, size));
    }

    @Override
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<CreateUserOut> createUser(@Valid @RequestBody CreateUserIn data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(data));
    }

    @Override
    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> updateUser(@PathVariable UUID userId, @Valid @RequestBody UpdateUserIn data) {
        userService.update(userId, data);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PageOut<UserListItemOut>> list(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "6") int size) {
        return ResponseEntity.ok(userService.listUsers(page, size));
    }

    @Override
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        userService.delete(userId);
        return ResponseEntity.noContent().build();
    }
}
