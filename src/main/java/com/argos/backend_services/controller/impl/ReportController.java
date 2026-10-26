package com.argos.backend_services.controller.impl;

import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.exception.PropertyNotFoundException;
import com.argos.backend_services.security.CurrentUserData;
import com.argos.backend_services.service.ReportHerdService;
import lombok.RequiredArgsConstructor;
import com.argos.backend_services.entity.HerdReport;
import com.argos.backend_services.entity.MedicineReport;
import com.argos.backend_services.service.ReportMedicineService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController {

    private final ReportMedicineService reportMedicineService;
    private final ReportHerdService reportHerdService;

    @GetMapping("/medicines/{medicineId}")
    public ResponseEntity<List<MedicineReport>> medicine(@CurrentUser CurrentUserData currentUserData) throws PropertyNotFoundException {
        return ResponseEntity.ok(reportMedicineService.readMedicine(currentUserData.id(), currentUserData.propertyId()));
    }

    @GetMapping("/herds/{herdId}")
    public ResponseEntity<HerdReport> herd(@CurrentUser CurrentUserData currentUserData) throws PropertyNotFoundException {
        return ResponseEntity.ok(reportHerdService.readHerd(currentUserData.id(), currentUserData.propertyId()));
    }

    @PutMapping("/sync/medicines/{medicineId}")
    public ResponseEntity<Void> syncMedicine(@CurrentUser CurrentUserData currentUserData) {
        reportMedicineService.synchronizeDaily();
        return ResponseEntity.ok().build();
    }

}
