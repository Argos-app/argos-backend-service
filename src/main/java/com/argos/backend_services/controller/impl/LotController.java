package com.argos.backend_services.controller.impl;

import com.argos.backend_services.controller.LotControllerApi;
import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.dto.in.LotIn;
import com.argos.backend_services.dto.out.LotOut;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.exception.PropertyNotFoundException;
import com.argos.backend_services.service.LotService;
import com.argos.backend_services.security.CurrentUserData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/lot")
@RequiredArgsConstructor
public class LotController implements LotControllerApi {

    private final LotService lotService;

    @Override
    public ResponseEntity<List<LotOut>> listLot(@CurrentUser CurrentUserData currentUser) {
        return ResponseEntity.ok(lotService.listLot(currentUser.id()));
    }

    @Override
    public ResponseEntity<Void> registerLot(@RequestBody @Valid LotIn lotIn,
                                            @CurrentUser CurrentUserData currentUser) throws LotNotFoundException, PropertyNotFoundException {
        lotService.registerLot(lotIn, currentUser.id(), currentUser.propertyId());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteLot(@PathVariable UUID lotId,
                                           @CurrentUser CurrentUserData currentUser) throws LotNotFoundException {
        lotService.deleteLot(lotId, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
