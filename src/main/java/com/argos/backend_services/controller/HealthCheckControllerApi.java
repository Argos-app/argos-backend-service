package com.argos.backend_services.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Tag(name = "Health Check", description = "Verificação de saúde do serviço")
public interface HealthCheckControllerApi {

    @GetMapping("/health")
    @Operation(summary = "Verifica a saúde do serviço")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Serviço em funcionamento"))
    ResponseEntity<Map<String, String>> healthCheck();
}
