package com.argos.backend_services.controller;

import com.argos.backend_services.dto.in.ApplicationIn;
import com.argos.backend_services.dto.out.ApplicationOut;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.exception.MedicineNotFoundException;
import com.argos.backend_services.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/application")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService service;

    @PostMapping
    public ResponseEntity<Void> createApplication(
            @RequestHeader UUID userId,
            @RequestBody @Valid ApplicationIn applicationIn) throws LotNotFoundException, MedicineNotFoundException {

        service.registerApplication(applicationIn, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping({"/list-all"})
    public ResponseEntity<List<ApplicationOut>> listAll() {
        return ResponseEntity.ok(service.listApplication());
    }
}
