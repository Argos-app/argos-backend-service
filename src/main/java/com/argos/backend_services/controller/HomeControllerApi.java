package com.argos.backend_services.controller;

import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.dto.out.HomeApplicationOut;
import com.argos.backend_services.security.CurrentUserData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping("/home")
@Tag(name = "Home", description = "Dados do painel inicial")
public interface HomeControllerApi {

    @GetMapping("/application-calendar")
    @Operation(summary = "Consulta o calendário de aplicações do usuário autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Calendário consultado"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    })
    ResponseEntity<List<HomeApplicationOut>> applicationCalendar(@Parameter(hidden = true) @CurrentUser CurrentUserData currentUser);
}
