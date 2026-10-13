package com.argos.backend_services.controller.impl;

import com.argos.backend_services.controller.HomeControllerApi;
import com.argos.backend_services.controller.annotation.CurrentUser;
import com.argos.backend_services.dto.out.HomeApplicationOut;
import com.argos.backend_services.security.CurrentUserData;
import com.argos.backend_services.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController implements HomeControllerApi {

    private final HomeService homeService;

    @Override
    public ResponseEntity<List<HomeApplicationOut>> applicationCalendar(@CurrentUser CurrentUserData currentUser) {
        return ResponseEntity.ok(homeService.findApplicationCalendar(currentUser.id()));
    }
}
