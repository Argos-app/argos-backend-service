package com.argos.backend_services.service;

import com.argos.backend_services.adapter.FirebaseMessagingAdapter;
import com.argos.backend_services.dto.out.ApplicationWithdrawalOut;
import com.argos.backend_services.repository.DeviceRepository;
import com.argos.backend_services.repository.NotificationRepository;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalNotificationService {

    private static final int MAX_ATTEMPTS = 3;
    private static final int BATCH_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final DeviceRepository deviceRepository;
    private final FirebaseMessagingAdapter firebaseMessagingAdapter;

    @Transactional
    public void prepare(List<ApplicationWithdrawalOut> applications) {
        for (var application : applications) {
            UUID alertId = stableId("withdrawal-due", application.getApplicationId(),
                    application.getMeatWithdrawalEnd().toString());
            String title = "Carência encerrada: lote " + application.getLotName();
            String message = "A carência do lote " + application.getLotName() + " terminou em "
                    + application.getMeatWithdrawalEnd() + ".";
            notificationRepository.createAlert(alertId, application.getUserId(), application.getLotId(),
                    title, message);
            for (var device : deviceRepository.findByUser_IdAndActiveTrueAndPushTokenIsNotNull(application.getUserId())) {
                UUID deliveryId = stableId("withdrawal-push", alertId, device.getId().toString());
                notificationRepository.createDelivery(deliveryId, alertId, device.getId());
            }
        }
    }

    public void sendPending() {
        // A próxima execução do agendamento recupera entregas pendentes após uma interrupção.
        for (var delivery : notificationRepository.findRetryable(MAX_ATTEMPTS, BATCH_SIZE)) {
            if (!notificationRepository.claim(delivery)) {
                continue;
            }
            try {
                firebaseMessagingAdapter.send(delivery);
                notificationRepository.markSent(delivery.id());
            } catch (FirebaseMessagingException exception) {
                String errorCode = exception.getMessagingErrorCode() == null
                        ? "FCM_ERROR" : exception.getMessagingErrorCode().name();
                notificationRepository.markFailed(delivery.id(), errorCode);
                if (exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    notificationRepository.deactivateInvalidDevice(delivery.deviceId(), delivery.pushToken());
                }
                log.warn("withdrawal_push_failed deliveryId={} errorCode={}", delivery.id(), errorCode);
            } catch (RuntimeException exception) {
                notificationRepository.markFailed(delivery.id(), "FCM_UNEXPECTED_ERROR");
                log.error("withdrawal_push_failed deliveryId={} errorCode=FCM_UNEXPECTED_ERROR", delivery.id());
            }
        }
    }

    private UUID stableId(String event, UUID resourceId, String detail) {
        return UUID.nameUUIDFromBytes((event + ":" + resourceId + ":" + detail)
                .getBytes(StandardCharsets.UTF_8));
    }
}
