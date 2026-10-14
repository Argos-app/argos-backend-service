package com.argos.backend_services.controller;

import com.argos.backend_services.dto.in.MedicineBatchIn;
import com.argos.backend_services.dto.out.MedicineCreatedOut;
import com.argos.backend_services.dto.out.MedicineListItemOut;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.service.MedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MedicineController implements MedicineControllerApi {

    private final MedicineService medicineService;

    @Override
    public ResponseEntity<MedicineCreatedOut> create(MedicineBatchIn input, UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicineService.registerBatch(input, userId));
    }

    @Override
    public ResponseEntity<PageOut<MedicineListItemOut>> list(UUID therapeuticCategoryId, int page, int size) {
        return ResponseEntity.ok(medicineService.list(therapeuticCategoryId, page, size));
    }
}
