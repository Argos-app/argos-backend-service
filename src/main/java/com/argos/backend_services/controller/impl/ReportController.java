package com.argos.backend_services.controller.impl;

import com.argos.backend_services.controller.ReportControllerApi;
import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.exception.PropertyNotFoundException;
import com.argos.backend_services.security.CurrentUserData;
import com.argos.backend_services.service.ReportHerdService;
import lombok.RequiredArgsConstructor;
import com.argos.backend_services.entity.HerdReport;
import com.argos.backend_services.entity.MedicineReport;
import com.argos.backend_services.service.ReportMedicineService;
import com.argos.backend_services.service.HerdReportExportService;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController implements ReportControllerApi {

    private final ReportMedicineService reportMedicineService;
    private final ReportHerdService reportHerdService;
    private final HerdReportExportService herdReportExportService;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<byte[]> export(@PathVariable String reportId,
                                            @CurrentUser CurrentUserData currentUser) {
        byte[] workbook = herdReportExportService.export(reportId, currentUser.id());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("relatorio-rebanho.xlsx").build().toString())
                .cacheControl(CacheControl.noStore())
                .body(workbook);
    }

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
