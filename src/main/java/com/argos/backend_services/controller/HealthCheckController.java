package com.argos.backend_services.controller;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@Tag(name = "Health Check", description = "Rota de verificação de saúde do serviço")
public class HealthCheckController {
  @GetMapping("/health")
  @Operation(summary = "Verificação de saúde do serviço", description = "Verifica se o serviço está em funcionamento")
  public ResponseEntity<Map<String, String>> healthCheck() {
    return ResponseEntity.ok(Map.of("status", "UP", "service", "argos-backend-service"));
  }
}
