package com.argos.backend_services.service;

import com.argos.backend_services.repository.MedicineBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MedicineExpirationService {

    private final MedicineBatchRepository medicineBatchRepository;
    private final Clock clock;

    @Transactional
    public int markExpiredBatches() {
        return medicineBatchRepository.markAvailableBatchesExpired(LocalDate.now(clock));
    }
}
