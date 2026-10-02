package com.argos.backend_services.controller;

import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.dto.in.CreateUserIn;
import com.argos.backend_services.dto.in.UpdateUserIn;
import com.argos.backend_services.dto.out.CreateUserOut;
import com.argos.backend_services.dto.out.UserListItemOut;
import com.argos.backend_services.dto.out.UserCreationOptionsOut;
import com.argos.backend_services.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Rotas de gestão de usuários")
public class UserController {
    private final UserService userService;

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Lista opções para criação de usuário")
    public ResponseEntity<UserCreationOptionsOut> getCreationOptions() {
        return ResponseEntity.ok(userService.getCreationOptions());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Cria um usuário", description = "Cria usuário no Firebase e o vincula a uma propriedade")
    public ResponseEntity<CreateUserOut> createUser(@Valid @RequestBody CreateUserIn data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(data));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Atualiza os dados e o vínculo de um usuário")
    public ResponseEntity<Void> updateUser(
            @PathVariable java.util.UUID userId,
            @Valid @RequestBody UpdateUserIn data) {
        userService.update(userId, data);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Lista usuários vinculados a fazendas",
            description = "Retorna os usuários com dados de perfil e vínculo à fazenda, paginado com 6 usuários por página"
    )
    public ResponseEntity<PageOut<UserListItemOut>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        return ResponseEntity.ok(userService.listUsers(page, size));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Remove um usuário",
            description = "Remove um usuário do sistema, desativando seu acesso"
    )
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        userService.delete(userId);
        return ResponseEntity.noContent().build();
    }
}
