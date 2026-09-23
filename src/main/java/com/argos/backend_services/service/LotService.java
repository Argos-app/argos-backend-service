package com.argos.backend_services.service;

import com.argos.backend_services.repository.LotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LotService {

    private LotRepository lotRepository;
}
