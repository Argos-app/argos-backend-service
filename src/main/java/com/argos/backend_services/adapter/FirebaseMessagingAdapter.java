package com.argos.backend_services.adapter;

import com.argos.backend_services.repository.NotificationRepository.Delivery;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FirebaseMessagingAdapter {

    private final ObjectProvider<FirebaseApp> firebaseApp;

    public void send(Delivery delivery) throws FirebaseMessagingException {
        var message = Message.builder()
                .setToken(delivery.pushToken())
                .setNotification(Notification.builder()
                        .setTitle(delivery.title())
                        .setBody(delivery.message())
                        .build())
                .putData("type", "CARENCIA")
                .putData("alertId", delivery.alertId().toString())
                .putData("lotId", delivery.lotId().toString())
                .build();
        FirebaseMessaging.getInstance(firebaseApp.getObject()).send(message);
    }
}
