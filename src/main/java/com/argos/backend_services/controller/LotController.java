package com.argos.backend_services.controller;

import com.argos.backend_services.dto.in.LotIn;
import com.argos.backend_services.dto.out.LotOut;
import com.argos.backend_services.exception.LotNotFoundException;
import com.argos.backend_services.service.LotService;
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
    public ResponseEntity<List<LotOut>> listLot(@RequestHeader UUID userId) {
        return ResponseEntity.ok(lotService.listLot(userId));
    }

    @Override
    public ResponseEntity<Void> registerLot(@RequestBody @Valid LotIn lotIn) throws LotNotFoundException {
        lotService.registerLot(lotIn);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteLot(@PathVariable UUID lotId, @RequestHeader UUID userId) throws LotNotFoundException {
        lotService.deleteLot(lotId, userId);
        return ResponseEntity.noContent().build();
    }
}
