package com.argos.backend_services.controller.impl;

import com.argos.backend_services.controller.ApplicationControllerApi;
import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.dto.in.ApplicationIn;
import com.argos.backend_services.dto.out.ApplicationOut;
import com.argos.backend_services.dto.out.ApplicationWithdrawalOut;
import com.argos.backend_services.exception.InvalidDateWindowException;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.exception.MedicineNotFoundException;
import com.argos.backend_services.security.CurrentUserData;
import com.argos.backend_services.service.ApplicationService;
import com.argos.backend_services.service.ApplicationWithdrawalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ApplicationController implements ApplicationControllerApi {

    private final ApplicationService service;
    private final ApplicationWithdrawalService withdrawalService;

    @Override
    public ResponseEntity<Void> createApplication(@CurrentUser CurrentUserData currentUser,
                                                 @RequestBody @Valid ApplicationIn applicationIn) throws LotNotFoundException, MedicineNotFoundException {

        service.registerApplication(applicationIn, currentUser.id());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<ApplicationOut>> listAll(@CurrentUser CurrentUserData currentUser) {
        return ResponseEntity.ok(service.listApplication(currentUser.id()));
    }

    @Override
    public ResponseEntity<List<ApplicationWithdrawalOut>> withdrawalWindow(
            @RequestParam(required = false) LocalDate referenceDate,
            @RequestParam(defaultValue = "7") int daysBefore,
            @RequestParam(defaultValue = "7") int daysAfter,
            @CurrentUser CurrentUserData currentUser) throws InvalidDateWindowException {
        var date = referenceDate == null ? LocalDate.now() : referenceDate;
        return ResponseEntity.ok(withdrawalService.findNearDate(date, daysBefore, daysAfter, currentUser.id()));
    }
}
