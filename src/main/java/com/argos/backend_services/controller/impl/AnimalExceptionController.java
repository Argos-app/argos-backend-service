package com.argos.backend_services.controller.impl;

import com.argos.backend_services.controller.AnimalExceptionControllerApi;
import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.dto.in.AnimalExceptionIn;
import com.argos.backend_services.dto.out.AnimalExceptionOut;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.service.AnimalExceptionService;
import com.argos.backend_services.security.CurrentUserData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/animal-exceptions")
@RequiredArgsConstructor
public class AnimalExceptionController implements AnimalExceptionControllerApi {

    private final AnimalExceptionService animalExceptionService;

    @Override
    public ResponseEntity<Void> create(
            AnimalExceptionIn input,
            @CurrentUser CurrentUserData currentUser) throws LotNotFoundException {
        animalExceptionService.register(input, currentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<PageOut<AnimalExceptionOut>> list(
            Boolean active,
            UUID originId,
            int page,
            int size,
            @CurrentUser CurrentUserData currentUser) {
        return ResponseEntity.ok(animalExceptionService.list(active, originId, currentUser.id(), page, size));
    }
}
