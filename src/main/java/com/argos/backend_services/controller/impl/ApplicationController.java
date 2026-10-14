package com.argos.backend_services.controller.impl;

import com.argos.backend_services.controller.ApplicationControllerApi;
import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.dto.in.ApplicationIn;
import com.argos.backend_services.dto.out.ApplicationHistoryOut;
import com.argos.backend_services.dto.out.ApplicationOut;
import com.argos.backend_services.dto.out.ApplicationWithdrawalOut;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.exception.InvalidDateWindowException;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.exception.MedicineNotFoundException;
import com.argos.backend_services.service.ApplicationService;
import com.argos.backend_services.service.ApplicationWithdrawalService;
import com.argos.backend_services.security.CurrentUserData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

@RestController
@RequestMapping("/application")
@RequiredArgsConstructor
public class ApplicationController implements ApplicationControllerApi {

    private final ApplicationService service;
    private final ApplicationWithdrawalService withdrawalService;

    @Override
    public ResponseEntity<Void> createApplication(@RequestHeader UUID userId,
                                                 @RequestBody @Valid ApplicationIn applicationIn) throws LotNotFoundException, MedicineNotFoundException {

        service.registerApplication(applicationIn, userId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<ApplicationOut>> listAll() {
        return ResponseEntity.ok(service.listApplication());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public ResponseEntity<PageOut<ApplicationHistoryOut>> applicationHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @CurrentUser CurrentUserData currentUser) {
        return ResponseEntity.ok(service.findHistory(page, size, currentUser.id()));
    }

    @Override
    public ResponseEntity<List<ApplicationWithdrawalOut>> withdrawalWindow(
            @RequestParam(required = false) LocalDate referenceDate,
            @RequestParam(defaultValue = "7") int daysBefore,
            @RequestParam(defaultValue = "7") int daysAfter) throws InvalidDateWindowException {
        var date = referenceDate == null ? LocalDate.now() : referenceDate;
        return ResponseEntity.ok(withdrawalService.findNearDate(date, daysBefore, daysAfter));
    }
}
