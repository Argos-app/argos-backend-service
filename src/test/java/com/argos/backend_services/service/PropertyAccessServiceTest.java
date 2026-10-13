package com.argos.backend_services.service;

import com.argos.backend_services.entity.Property;
import com.argos.backend_services.entity.UserProperty;
import com.argos.backend_services.repository.UserPropertyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PropertyAccessServiceTest {

    @Test
    void shouldAllowAnActiveUserPropertyLink() {
        var repository = mock(UserPropertyRepository.class);
        var service = new PropertyAccessService(repository);
        var userId = UUID.randomUUID();
        var propertyId = UUID.randomUUID();
        when(repository.existsByUser_IdAndProperty_IdAndActiveTrue(userId, propertyId)).thenReturn(true);

        service.requireActiveAccess(userId, propertyId);

        verify(repository).existsByUser_IdAndProperty_IdAndActiveTrue(userId, propertyId);
    }

    @Test
    void shouldDenyWhenNoActiveUserPropertyLinkExists() {
        var repository = mock(UserPropertyRepository.class);
        var service = new PropertyAccessService(repository);

        assertThatThrownBy(() -> service.requireActiveAccess(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldReturnOnlyPropertiesFromActiveLinks() {
        var repository = mock(UserPropertyRepository.class);
        var service = new PropertyAccessService(repository);
        var userId = UUID.randomUUID();
        var firstPropertyId = UUID.randomUUID();
        var secondPropertyId = UUID.randomUUID();
        when(repository.findByUser_IdAndActiveTrue(userId)).thenReturn(List.of(
                UserProperty.builder().property(Property.builder().id(firstPropertyId).build()).build(),
                UserProperty.builder().property(Property.builder().id(secondPropertyId).build()).build()));

        assertThat(service.activePropertyIds(userId)).containsExactly(firstPropertyId, secondPropertyId);
    }
}
