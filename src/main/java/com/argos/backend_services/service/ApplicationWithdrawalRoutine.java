package com.argos.backend_services.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationWithdrawalRoutine {

    private final ApplicationWithdrawalService withdrawalService;

    @Scheduled(cron = "${argos.routines.withdrawal.cron:0 0 8 * * *}", zone = "${argos.routines.time-zone:America/Sao_Paulo}")
    public void notifyWithdrawalsDueToday() {
        var dueApplications = withdrawalService.findDueToday();
        log.info("Found {} application withdrawal notifications for today", dueApplications.size());
    }
}