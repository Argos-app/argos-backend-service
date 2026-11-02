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
    private final WithdrawalNotificationService notificationService;

    @Scheduled(cron = "${argos.routines.withdrawal.cron:0 0 8 * * *}", zone = "${argos.routines.time-zone:America/Sao_Paulo}")
    public void notifyWithdrawalsDueToday() {
        var dueApplications = withdrawalService.findDueToday();
        notificationService.prepare(dueApplications);
        notificationService.sendPending();
        log.info("withdrawal_notification_routine applications={}", dueApplications.size());
    }

    @Scheduled(cron = "${argos.routines.withdrawal.retry-cron:0 */15 * * * *}",
            zone = "${argos.routines.time-zone:America/Sao_Paulo}")
    public void retryPendingNotifications() {
        notificationService.sendPending();
    }
}
