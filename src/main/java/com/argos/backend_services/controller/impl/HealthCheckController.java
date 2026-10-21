package com.argos.backend_services.controller.impl;

import java.util.Map;

import com.argos.backend_services.controller.HealthCheckControllerApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController()
public class HealthCheckController implements HealthCheckControllerApi {
  @Override
  public ResponseEntity<Map<String, String>> healthCheck() {
    return ResponseEntity.ok(Map.of("status", "UP", "service", "argos-backend-service"));
  }
}
