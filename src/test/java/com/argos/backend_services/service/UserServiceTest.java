package com.argos.backend_services.service;

import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.dto.out.UserListItemOut;
import com.argos.backend_services.exception.BadRequestException;
import com.argos.backend_services.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void shouldListUsersPaginatedWithSixItemsPerPage() {
        UUID userId = UUID.randomUUID();
        UserListItemOut item = new UserListItemOut(
                userId,
                "https://example.com/photo.jpg",
                "Argos User",
                "user@argos.com",
                true,
                "Fazenda Sao Joao",
                "12345678000190",
                LocalDate.of(2026, 1, 15)
        );

        Page<UserListItemOut> page = new PageImpl<>(List.of(item), PageRequest.of(0, 6), 7);

        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findAllLinked(PageRequest.of(0, 6))).thenReturn(page);

        UserService userService = new UserService(userRepository);
        PageOut<UserListItemOut> result = userService.listUsers(0);

        assertEquals(1, result.data().size());
        assertEquals(0, result.page());
        assertEquals(6, result.size());
        assertEquals(7, result.totalElements());
        assertEquals(2, result.totalPages());
        assertTrue(result.first());
        assertFalse(result.last());

        UserListItemOut resultItem = result.data().getFirst();
        assertEquals(userId, resultItem.userId());
        assertEquals("https://example.com/photo.jpg", resultItem.photoUrl());
        assertEquals("Argos User", resultItem.name());
        assertEquals("user@argos.com", resultItem.email());
        assertTrue(resultItem.active());
        assertEquals("Fazenda Sao Joao", resultItem.farmName());
        assertEquals("12345678000190", resultItem.cnpj());
        assertEquals(LocalDate.of(2026, 1, 15), resultItem.linkDate());

        verify(userRepository).findAllLinked(eq(PageRequest.of(0, 6)));
    }

    @Test
    void shouldRejectNegativePage() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserService(userRepository);

        assertThrows(BadRequestException.class, () -> userService.listUsers(-1));
    }
}
