package com.argos.backend_services.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MedicineExpirationRoutine {

    private final MedicineExpirationService medicineExpirationService;

    @Scheduled(cron = "${argos.routines.medicine-expiration.cron:0 0 8 * * *}",
            zone = "${argos.routines.time-zone:America/Sao_Paulo}")
    public void expireMedicineBatches() {
        var expiredBatches = medicineExpirationService.markExpiredBatches();
        log.info("Marked {} medicine batches as expired", expiredBatches);
    }
}
