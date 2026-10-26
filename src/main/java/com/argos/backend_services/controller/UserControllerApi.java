package com.argos.backend_services.controller;

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
import com.argos.backend_services.security.CurrentUserData;
import com.argos.backend_services.controller.annotation.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/users")
@Tag(name = "Users", description = "Rotas de gestão de usuários e seus acessos")
public interface UserControllerApi {

    @GetMapping("/me")
    @Operation(summary = "Consulta o perfil do usuário autenticado",
            description = "Retorna dados pessoais, acesso atual e todos os vínculos ativos em fazendas ativas do próprio usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil encontrado", content = @Content(schema = @Schema(implementation = UserProfileOut.class))),
            @ApiResponse(responseCode = "401", description = "Sessão ausente/inválida, usuário inexistente ou conta/permissão inativa")
    })
    ResponseEntity<UserProfileOut> getMyProfile(
            @Parameter(hidden = true) @CurrentUser CurrentUserData currentUser);

    @GetMapping("/options")
    @Operation(summary = "Lista opções para criação e edição de usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções encontradas", content = @Content(schema = @Schema(implementation = UserCreationOptionsOut.class))),
            @ApiResponse(responseCode = "403", description = "Acesso permitido somente a administradores")
    })
    ResponseEntity<UserCreationOptionsOut> getCreationOptions(
            @Parameter(hidden = true) @CurrentUser CurrentUserData currentUser);

    @GetMapping("/permissions")
    @Operation(summary = "Lista permissões ativas disponíveis para atribuição")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permissões encontradas", content = @Content(schema = @Schema(implementation = UserOptionOut.class))),
            @ApiResponse(responseCode = "403", description = "Acesso permitido somente a administradores")
    })
    ResponseEntity<List<UserOptionOut>> listPermissions();

    @GetMapping("/accesses")
    @Operation(summary = "Lista usuários e seus acessos de forma paginada", description = "Retorna uma linha por usuário com vínculo ativo em fazenda ativa sob responsabilidade do administrador autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acessos encontrados", content = @Content(schema = @Schema(implementation = PageOut.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso permitido somente a administradores")
    })
    ResponseEntity<PageOut<UserAccessOut>> listAccesses(
            @Parameter(description = "Índice da página, começando em zero") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Quantidade de registros por página, entre 1 e 100") @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true) @CurrentUser CurrentUserData currentUser);

    @PatchMapping("/{userId}/access")
    @Operation(summary = "Substitui a permissão de um usuário", description = "Cada usuário possui uma única permissão. A nova permissão deve estar ativa.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Permissão atualizada"),
            @ApiResponse(responseCode = "400", description = "Identificador ou dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso permitido somente a administradores"),
            @ApiResponse(responseCode = "404", description = "Usuário ou permissão não encontrados/ativos"),
            @ApiResponse(responseCode = "409", description = "Conta vinculada a fazendas de outros responsáveis")
    })
    ResponseEntity<Void> updateAccess(@Parameter(description = "Identificador do usuário") @PathVariable UUID userId,
                                      @Valid @RequestBody UpdateUserAccessIn data,
                                      @Parameter(hidden = true) @CurrentUser CurrentUserData currentUser);

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Ativa ou desativa um usuário", description = "Altera somente o status da conta e preserva seus vínculos com fazendas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Status atualizado"),
            @ApiResponse(responseCode = "400", description = "Identificador ou dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso permitido somente a administradores"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "409", description = "Conta vinculada a fazendas de outros responsáveis")
    })
    ResponseEntity<Void> updateStatus(@Parameter(description = "Identificador do usuário") @PathVariable UUID userId,
                                      @Valid @RequestBody UpdateUserStatusIn data,
                                      @Parameter(hidden = true) @CurrentUser CurrentUserData currentUser);

    @GetMapping("/admins")
    @Operation(summary = "Retorna o administrador autenticado para seleção de responsável")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Administradores encontrados", content = @Content(schema = @Schema(implementation = PageOut.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso permitido somente a administradores")
    })
    ResponseEntity<PageOut<AdminUserOptionOut>> listAdministrators(
            @Parameter(description = "Índice da página, começando em zero") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Quantidade de registros por página") @RequestParam(defaultValue = "6") int size,
            @Parameter(hidden = true) @CurrentUser CurrentUserData currentUser);

    @PostMapping
    @Operation(summary = "Cria um usuário", description = "Cria a conta no Firebase e vincula o usuário a uma propriedade.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado", content = @Content(schema = @Schema(implementation = CreateUserOut.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou usuário já cadastrado"),
            @ApiResponse(responseCode = "403", description = "Acesso permitido somente a administradores")
    })
    ResponseEntity<CreateUserOut> createUser(@Valid @RequestBody CreateUserIn data,
            @Parameter(hidden = true) @CurrentUser CurrentUserData currentUser);

    @PutMapping("/{userId}")
    @Operation(summary = "Atualiza os dados pessoais e o vínculo de um usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou duplicados"),
            @ApiResponse(responseCode = "403", description = "Acesso permitido somente a administradores"),
            @ApiResponse(responseCode = "404", description = "Usuário, propriedade ou vínculo não encontrado"),
            @ApiResponse(responseCode = "409", description = "Conta vinculada a fazendas de outros responsáveis")
    })
    ResponseEntity<Void> updateUser(@Parameter(description = "Identificador do usuário") @PathVariable UUID userId,
                                    @Valid @RequestBody UpdateUserIn data,
                                    @Parameter(hidden = true) @CurrentUser CurrentUserData currentUser);

    @GetMapping
    @Operation(summary = "Lista usuários vinculados a fazendas", description = "Pagina usuários únicos e retorna suas fazendas ativas sob responsabilidade do administrador autenticado, em ordem alfabética.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuários encontrados", content = @Content(schema = @Schema(implementation = PageOut.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso permitido somente a administradores")
    })
    ResponseEntity<PageOut<UserListItemOut>> list(
            @Parameter(description = "Índice da página, começando em zero") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Quantidade de registros por página") @RequestParam(defaultValue = "6") int size,
            @Parameter(hidden = true) @CurrentUser CurrentUserData currentUser);

    @DeleteMapping("/{userId}")
    @Operation(summary = "Remove vínculos do usuário", description = "Desativa vínculos ativos somente nas fazendas do administrador. Preserva a conta global e vínculos de outros responsáveis.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Acesso removido"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso permitido somente a administradores"),
            @ApiResponse(responseCode = "404", description = "Usuário ou vínculo ativo não encontrado")
    })
    ResponseEntity<Void> deleteUser(@Parameter(description = "Identificador do usuário") @PathVariable String userId,
            @Parameter(hidden = true) @CurrentUser CurrentUserData currentUser);

    @PatchMapping("/update-profile")
    @Operation(summary = "Atualiza o perfil do usuário autenticado",
            description = "Atualiza nome, e-mail e/ou URL da foto. Os campos não enviados são preservados.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Perfil atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos, vazios ou duplicados"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    ResponseEntity<Void> updateCurrentUser(
            @Valid @RequestBody UpdateCurrentUserIn data,
            @Parameter(hidden = true) @CurrentUser CurrentUserData currentUser);

}
