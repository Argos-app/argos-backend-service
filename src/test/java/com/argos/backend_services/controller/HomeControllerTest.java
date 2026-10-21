package com.argos.backend_services.controller;

import com.argos.backend_services.controller.impl.HomeController;
import com.argos.backend_services.dto.out.HomeApplicationOut;
import com.argos.backend_services.security.CurrentUserData;
import com.argos.backend_services.service.HomeService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HomeControllerTest {

    @Test
    void shouldReturnCalendarForAuthenticatedUser() {
        // Arrange
        var homeService = mock(HomeService.class);
        var controller = new HomeController(homeService);
        var userId = UUID.randomUUID();
        var currentUser = new CurrentUserData(userId, "user@argos.com", "User", "USER", null);
        var expected = List.of(HomeApplicationOut.builder().applicationId(UUID.randomUUID()).build());
        when(homeService.findApplicationCalendar(userId)).thenReturn(expected);

        // Act
        var response = controller.applicationCalendar(currentUser);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(expected);
        verify(homeService).findApplicationCalendar(userId);
    }
}
