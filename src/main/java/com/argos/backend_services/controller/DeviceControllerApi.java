package com.argos.backend_services.controller;

import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.dto.in.DeviceSyncIn;
import com.argos.backend_services.dto.out.DeviceSyncOut;
import com.argos.backend_services.security.CurrentUserData;
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
@Tag(name = "Devices", description = "Sincronização de dispositivos para notificações push")
public interface DeviceControllerApi {
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
            @Parameter(hidden = true) @CurrentUser CurrentUserData user);

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
