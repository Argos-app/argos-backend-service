package com.argos.backend_services.controller.impl;

import com.argos.backend_services.controller.MedicineControllerApi;
import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.dto.in.MedicineBatchIn;
import com.argos.backend_services.dto.out.MedicineCreatedOut;
import com.argos.backend_services.dto.out.MedicineCatalogItemOut;
import com.argos.backend_services.dto.out.MedicineListItemOut;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.service.MedicineService;
import com.argos.backend_services.security.CurrentUserData;
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
    public ResponseEntity<MedicineCreatedOut> create(MedicineBatchIn input,
                                                      @CurrentUser CurrentUserData currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicineService.registerBatch(input, currentUser.id()));
    }

    @Override
    public ResponseEntity<PageOut<MedicineListItemOut>> list(UUID therapeuticCategoryId, int page, int size,
                                                              @CurrentUser CurrentUserData currentUser) {
        return ResponseEntity.ok(medicineService.list(therapeuticCategoryId, page, size, currentUser.id()));
    }

    @Override
    public ResponseEntity<PageOut<MedicineCatalogItemOut>> catalog(int page, int size,
                                                                    @CurrentUser CurrentUserData currentUser) {
        return ResponseEntity.ok(medicineService.listCatalog(page, size));
    }
}
