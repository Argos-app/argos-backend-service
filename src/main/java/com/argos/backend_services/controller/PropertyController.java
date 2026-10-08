package com.argos.backend_services.controller;

import com.argos.backend_services.dto.in.PropertyIn;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.dto.out.PropertyOut;
import com.argos.backend_services.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
public class PropertyController implements PropertyControllerApi {
    private final PropertyService propertyService;

    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public ResponseEntity<PageOut<PropertyOut>> listActive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        return ResponseEntity.ok(propertyService.listActive(page, size));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public ResponseEntity<PropertyOut> create(@Valid @RequestBody PropertyIn input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(propertyService.create(input));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public ResponseEntity<Void> update(@PathVariable UUID propertyId, @Valid @RequestBody PropertyIn input) {
        propertyService.update(propertyId, input);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public ResponseEntity<Void> deactivate(@PathVariable UUID propertyId) {
        propertyService.deactivate(propertyId);
        return ResponseEntity.noContent().build();
    }
}
