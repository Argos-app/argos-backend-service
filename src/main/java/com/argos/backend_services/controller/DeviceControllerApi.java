package com.argos.backend_services.controller;

import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.controller.handler.ApiError;
import com.argos.backend_services.dto.in.DeviceSyncIn;
import com.argos.backend_services.dto.out.DeviceRegistrationOut;
import com.argos.backend_services.dto.out.DeviceSyncOut;
import com.argos.backend_services.exception.DeviceConflictException;
import com.argos.backend_services.security.CurrentUserData;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/devices")
@Tag(name = "Dispositivos", description = "Sincronização de dispositivos para notificações push")
public interface DeviceControllerApi {
    @GetMapping("/{deviceId}/registration")
    @Operation(summary = "Consulta o registro do dispositivo para notificações",
            description = "Considera registrado somente o dispositivo ativo, com token push, pertencente ao usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dispositivo ativo e com token push",
                    content = @Content(schema = @Schema(implementation = DeviceRegistrationOut.class),
                            examples = @ExampleObject(value = "{\"registered\":true}"))),
            @ApiResponse(responseCode = "400", description = "Dispositivo encontrado, porém inativo ou sem token push",
                    content = @Content(schema = @Schema(implementation = DeviceRegistrationOut.class),
                            examples = @ExampleObject(value = "{\"registered\":false}"))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou usuário não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Usuário autenticado sem permissão para consultar o dispositivo",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Dispositivo inexistente ou fora do escopo do usuário autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Banco indisponível ou operação excedeu o tempo limite; tentar novamente",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ResponseEntity<DeviceRegistrationOut> registrationStatus(@PathVariable UUID deviceId,
            @Parameter(hidden = true) @CurrentUser CurrentUserData user);

    @PostMapping("/sync")
    @Operation(summary = "Cadastra ou atualiza o dispositivo",
            description = "Enviar após login e renovação do token. device_id é opcional no primeiro cadastro.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dispositivo sincronizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "404", description = "Dispositivo inexistente ou fora do escopo"),
            @ApiResponse(responseCode = "409", description = "Token associado a outro registro"),
            @ApiResponse(responseCode = "503", description = "Timeout de transação ou contenção no banco; tentar novamente")
    })
    ResponseEntity<DeviceSyncOut> sync(@Valid @RequestBody DeviceSyncIn input,
            @Parameter(hidden = true) @CurrentUser CurrentUserData user) throws DeviceConflictException;

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Desativa o dispositivo e libera o token",
            description = "Executar antes de descartar a sessão no logout.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Dispositivo desativado"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "404", description = "Dispositivo inexistente ou fora do escopo"),
            @ApiResponse(responseCode = "503", description = "Timeout de transação ou contenção no banco; tentar novamente")
    })
    ResponseEntity<Void> deactivate(@PathVariable UUID deviceId,
            @Parameter(hidden = true) @CurrentUser CurrentUserData user);
}
