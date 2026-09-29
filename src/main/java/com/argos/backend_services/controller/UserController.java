package com.argos.backend_services.controller;

import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.dto.out.UserListItemOut;
import com.argos.backend_services.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Rotas de gestão de usuários")
public class UserController {
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Lista usuários vinculados a fazendas",
            description = "Retorna os usuários com dados de perfil e vínculo à fazenda, paginado com 6 usuários por página"
    )
    public ResponseEntity<PageOut<UserListItemOut>> list(
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(userService.listUsers(page));
    }
}
