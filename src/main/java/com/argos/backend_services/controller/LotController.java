package com.argos.backend_services.controller;

import com.argos.backend_services.dto.out.LotOut;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/lot")
@RequiredArgsConstructor
public class LotController {

    private final LotService service;

    @GetMapping("/list-all")
    public ResponseEntity<List<LotOut>> listLot(UUID userId) {
        return ResponseEntity.ok(service.listLot(userId));
    }

    @PostMapping
    public ResponseEntity<Void> registerLot(LotIn lotIn) {
        service.registerLot(lotIn);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{lot_id}")
    public ResponseEntity<Void> slaughterLote(@PathVariable UUID lotId) {
        service.deleteLot(lotId);
        return ResponseEntity.ok().build();
    }
}
