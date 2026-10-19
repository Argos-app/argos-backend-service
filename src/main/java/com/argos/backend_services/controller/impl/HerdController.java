package com.argos.backend_services.controller.impl;

import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.dto.HerdOut;
import com.argos.backend_services.security.CurrentUserData;
import com.argos.backend_services.service.HerdService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/herd")
@RequiredArgsConstructor
public class HerdController {

    private final HerdService herdService;

    @GetMapping("/list-all")
    public ResponseEntity<List<HerdOut>> listHerd(@CurrentUser CurrentUserData currentUserData) {
        return ResponseEntity.ok(herdService.listHerd(currentUserData.id(), currentUserData.propertyId()));
    }
}
