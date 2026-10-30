package com.argos.backend_services.controller.impl;

import com.argos.backend_services.controller.UserControllerApi;
import com.argos.backend_services.dto.in.CreateUserIn;
import com.argos.backend_services.dto.in.UpdateCurrentUserIn;
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
import com.argos.backend_services.dto.out.UserProfileOut;
import com.argos.backend_services.service.UserService;
import com.argos.backend_services.security.CurrentUserData;
import com.argos.backend_services.controller.annotation.CurrentUser;
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileOut> getMyProfile(@CurrentUser CurrentUserData currentUser) {
        return ResponseEntity.ok(userService.getMyProfile(currentUser.id()));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UserCreationOptionsOut> getCreationOptions(@CurrentUser CurrentUserData currentUser) {
        return ResponseEntity.ok(userService.getCreationOptions(currentUser.id()));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<UserOptionOut>> listPermissions() {
        return ResponseEntity.ok(userService.getActivePermissions());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PageOut<UserAccessOut>> listAccesses(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size,
                                                               @CurrentUser CurrentUserData currentUser) {
        return ResponseEntity.ok(userService.listUserAccesses(page, size, currentUser.id()));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> updateAccess(@PathVariable UUID userId, @Valid @RequestBody UpdateUserAccessIn data,
                                             @CurrentUser CurrentUserData currentUser) {
        userService.updateAccess(userId, data, currentUser.id());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID userId, @Valid @RequestBody UpdateUserStatusIn data,
                                             @CurrentUser CurrentUserData currentUser) {
        userService.updateStatus(userId, data, currentUser.id());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PageOut<AdminUserOptionOut>> listAdministrators(@RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "6") int size,
                                                                          @CurrentUser CurrentUserData currentUser) {
        return ResponseEntity.ok(userService.listAdministrators(page, size, currentUser.id()));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<CreateUserOut> createUser(@Valid @RequestBody CreateUserIn data,
                                                   @CurrentUser CurrentUserData currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(data, currentUser.id()));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> updateUser(@PathVariable UUID userId, @Valid @RequestBody UpdateUserIn data,
                                           @CurrentUser CurrentUserData currentUser) {
        userService.update(userId, data, currentUser.id());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PageOut<UserListItemOut>> list(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "6") int size,
                                                        @CurrentUser CurrentUserData currentUser) {
        return ResponseEntity.ok(userService.listUsers(page, size, currentUser.id()));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId,
                                           @CurrentUser CurrentUserData currentUser) {
        userService.delete(userId, currentUser.id());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> updateCurrentUser(@Valid @RequestBody UpdateCurrentUserIn data,
                                                  @CurrentUser CurrentUserData currentUser) {
        userService.updateCurrentUser(currentUser.id(), data);
        return ResponseEntity.noContent().build();
    }
}
