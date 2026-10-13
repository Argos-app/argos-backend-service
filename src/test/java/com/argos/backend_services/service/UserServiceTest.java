package com.argos.backend_services.service;

import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.dto.out.UserListItemOut;
import com.argos.backend_services.dto.out.UserProfilePropertyOut;
import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.Permission;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.entity.UserProperty;
import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.exception.UnauthorizedException;
import com.argos.backend_services.repository.UserRepository;
import com.argos.backend_services.repository.UserPropertyRepository;
import com.argos.backend_services.repository.PermissionRepository;
import com.argos.backend_services.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class UserServiceTest {
    private static final UUID ADMINISTRATOR_ID = UUID.randomUUID();

    @Test
    void shouldListUsersPaginatedWithSixItemsPerPage() {
        UUID userId = UUID.randomUUID();
        UserListItemOut item = new UserListItemOut(
                userId,
                "https://example.com/photo.jpg",
                "Argos User",
                "12345678901",
                "user@argos.com",
                true,
                UUID.randomUUID()
        );

        Page<UserListItemOut> page = new PageImpl<>(List.of(item), PageRequest.of(0, 6), 7);

        UserRepository userRepository = mock(UserRepository.class);
        UserPropertyRepository userPropertyRepository = mock(UserPropertyRepository.class);
        PermissionRepository permissionRepository = mock(PermissionRepository.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        when(userRepository.findLinkedByAdministrator(ADMINISTRATOR_ID, PageRequest.of(0, 6))).thenReturn(page);
        Property farm = Property.builder().id(UUID.randomUUID()).name("Fazenda Sao Joao")
                .cnpj("12345678000190").build();
        Property secondFarm = Property.builder().id(UUID.randomUUID()).name("Fazenda Vista Alegre").build();
        AppUser linkedUser = AppUser.builder().id(userId).build();
        when(userPropertyRepository.findActiveLinksByUsersAndAdministrator(List.of(userId), ADMINISTRATOR_ID))
                .thenReturn(List.of(
                        UserProperty.builder().user(linkedUser).property(farm).linkDate(LocalDate.of(2026, 1, 15)).build(),
                        UserProperty.builder().user(linkedUser).property(secondFarm).linkDate(LocalDate.of(2026, 2, 1)).build()));

        UserService userService = new UserService(userRepository, userPropertyRepository, permissionRepository, propertyRepository);
        PageOut<UserListItemOut> result = userService.listUsers(0, 6, ADMINISTRATOR_ID);

        assertEquals(1, result.data().size());
        assertEquals(0, result.page());
        assertEquals(6, result.size());
        assertEquals(7, result.totalElements());
        assertEquals(2, result.totalPages());
        assertTrue(result.first());
        assertFalse(result.last());

        UserListItemOut resultItem = result.data().get(0);
        assertEquals(userId, resultItem.userId());
        assertEquals("https://example.com/photo.jpg", resultItem.photoUrl());
        assertEquals("Argos User", resultItem.name());
        assertEquals("12345678901", resultItem.cpf());
        assertEquals("user@argos.com", resultItem.email());
        assertTrue(resultItem.active());
        assertEquals(2, resultItem.properties().size());
        assertEquals("Fazenda Sao Joao", resultItem.properties().getFirst().name());
        assertEquals("12345678000190", resultItem.properties().getFirst().cnpj());
        assertEquals(LocalDate.of(2026, 1, 15), resultItem.properties().getFirst().linkDate());

        verify(userRepository).findLinkedByAdministrator(eq(ADMINISTRATOR_ID), eq(PageRequest.of(0, 6)));
        verify(userPropertyRepository).findActiveLinksByUsersAndAdministrator(List.of(userId), ADMINISTRATOR_ID);
    }

    @Test
    void shouldRejectNegativePage() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserService(userRepository, mock(UserPropertyRepository.class),
                mock(PermissionRepository.class), mock(PropertyRepository.class));

        assertThrows(BadRequestException.class, () -> userService.listUsers(-1, 6, ADMINISTRATOR_ID));
    }

    @Test
    void shouldNotFetchLinksForEmptyUserPage() {
        UserRepository users = mock(UserRepository.class);
        UserPropertyRepository links = mock(UserPropertyRepository.class);
        when(users.findLinkedByAdministrator(ADMINISTRATOR_ID, PageRequest.of(0, 6)))
                .thenReturn(Page.empty(PageRequest.of(0, 6)));
        UserService service = new UserService(users, links, mock(PermissionRepository.class), mock(PropertyRepository.class));

        assertTrue(service.listUsers(0, 6, ADMINISTRATOR_ID).data().isEmpty());
        verifyNoInteractions(links);
    }

    @Test
    void shouldReturnCurrentProfileFieldsAndAllOwnActiveFarms() {
        UserRepository users = mock(UserRepository.class);
        UserPropertyRepository links = mock(UserPropertyRepository.class);
        UserService service = new UserService(users, links, mock(PermissionRepository.class), mock(PropertyRepository.class));
        UUID userId = UUID.randomUUID();
        Permission permission = Permission.builder().name("Operador").code("USER").active(true).build();
        AppUser user = AppUser.builder().id(userId).name("Nome atualizado").email("current@test.invalid")
                .cpf("12345678901").photoUrl("https://example.com/avatar.png")
                .permission(permission).active(true).build();
        var farms = List.of(new UserProfilePropertyOut(UUID.randomUUID(), "Alpha"),
                new UserProfilePropertyOut(UUID.randomUUID(), "Zeta"));
        when(users.findActiveProfileById(userId)).thenReturn(Optional.of(user));
        when(links.findActiveProfileProperties(userId)).thenReturn(farms);

        var result = service.getMyProfile(userId);

        assertEquals(userId, result.userId());
        assertEquals(user.getName(), result.name());
        assertEquals(user.getEmail(), result.email());
        assertEquals(user.getCpf(), result.cpf());
        assertEquals(user.getPhotoUrl(), result.photoUrl());
        assertEquals("Operador", result.permissionName());
        assertEquals("USER", result.permissionCode());
        assertTrue(result.active());
        assertEquals(farms, result.properties());
        verify(users).findActiveProfileById(userId);
        verify(links).findActiveProfileProperties(userId);
        verifyNoMoreInteractions(users, links);
    }

    @Test
    void shouldReturnProfileWithEmptyFarmsAndNullablePhoto() {
        UserRepository users = mock(UserRepository.class);
        UserPropertyRepository links = mock(UserPropertyRepository.class);
        UserService service = new UserService(users, links, mock(PermissionRepository.class), mock(PropertyRepository.class));
        UUID userId = UUID.randomUUID();
        when(users.findActiveProfileById(userId)).thenReturn(Optional.of(AppUser.builder().id(userId)
                .permission(Permission.builder().code("USER").name("Operador").build()).active(true).build()));
        when(links.findActiveProfileProperties(userId)).thenReturn(List.of());

        var result = service.getMyProfile(userId);

        assertTrue(result.properties().isEmpty());
        assertNull(result.photoUrl());
    }

    @Test
    void shouldRejectUnauthorizedProfileWithoutLoadingFarms() {
        UserRepository users = mock(UserRepository.class);
        UserPropertyRepository links = mock(UserPropertyRepository.class);
        UserService service = new UserService(users, links, mock(PermissionRepository.class), mock(PropertyRepository.class));
        UUID userId = UUID.randomUUID();
        when(users.findActiveProfileById(userId)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> service.getMyProfile(userId));
        verifyNoInteractions(links);
    }
}
