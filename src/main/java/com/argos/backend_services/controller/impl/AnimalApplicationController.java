package com.argos.backend_services.controller.impl;

import com.argos.backend_services.controller.AnimalApplicationControllerApi;
import com.argos.backend_services.dto.in.AnimalApplicationIn;
import com.argos.backend_services.exception.MedicineNotFoundException;
import com.argos.backend_services.service.AnimalApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/animal-application")
@RequiredArgsConstructor
public class AnimalApplicationController implements AnimalApplicationControllerApi {

    private final AnimalApplicationService animalApplicationService;

    @Override
    public ResponseEntity<Void> create(UUID animalExceptionId, AnimalApplicationIn input, UUID userId)
            throws MedicineNotFoundException {
        animalApplicationService.register(animalExceptionId, input, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
