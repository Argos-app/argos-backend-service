package com.argos.backend_services.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public void createAlert(UUID id, UUID userId, UUID lotId, String title, String message) {
        jdbcTemplate.update("""
                insert into alert (id, user_id, lot_id, type, severity, title, message)
                values (?, ?, ?, 'CARENCIA', 'MEDIA', ?, ?)
                on conflict (id) do nothing
                """, id, userId, lotId, title, message);
    }

    public void createDelivery(UUID id, UUID alertId, UUID deviceId) {
        jdbcTemplate.update("""
                insert into notification_delivery (id, alert_id, device_id, channel)
                values (?, ?, ?, 'PUSH')
                on conflict (id) do nothing
                """, id, alertId, deviceId);
    }

    public List<Delivery> findRetryable(int maxAttempts, int limit) {
        return jdbcTemplate.query("""
                select nd.id, nd.alert_id, nd.device_id, nd.attempts,
                       d.push_token, a.title, a.message, a.lot_id
                from notification_delivery nd
                join alert a on a.id = nd.alert_id
                join device d on d.id = nd.device_id and d.user_id = a.user_id
                join app_user u on u.id = a.user_id
                where nd.channel = 'PUSH'
                  and a.type = 'CARENCIA' and a.title like 'Carência encerrada:%'
                  and nd.status in ('PENDENTE', 'FALHOU')
                  and nd.attempts < ?
                  and d.active = true and d.push_token is not null and u.active = true
                order by nd.attempts, a.created_at, nd.id
                limit ?
                """, (rs, rowNum) -> new Delivery(
                rs.getObject("id", UUID.class), rs.getObject("alert_id", UUID.class),
                rs.getObject("device_id", UUID.class), rs.getInt("attempts"),
                rs.getString("push_token"), rs.getString("title"), rs.getString("message"),
                rs.getObject("lot_id", UUID.class)), maxAttempts, limit);
    }

    public boolean claim(Delivery delivery) {
        return jdbcTemplate.update("""
                update notification_delivery
                set attempts = attempts + 1, status = 'PENDENTE', error = null
                where id = ? and attempts = ? and status in ('PENDENTE', 'FALHOU')
                  and exists (select 1 from device
                              where id = ? and active = true and push_token = ?)
                """, delivery.id(), delivery.attempts(), delivery.deviceId(), delivery.pushToken()) == 1;
    }

    public void markSent(UUID deliveryId) {
        jdbcTemplate.update("""
                update notification_delivery
                set status = 'ENVIADO', sent_at = current_timestamp, error = null
                where id = ?
                """, deliveryId);
    }

    public void markFailed(UUID deliveryId, String errorCode) {
        jdbcTemplate.update("""
                update notification_delivery
                set status = 'FALHOU', error = ?
                where id = ?
                """, errorCode, deliveryId);
    }

    public void deactivateInvalidDevice(UUID deviceId, String pushToken) {
        jdbcTemplate.update("""
                update device
                set active = false, push_token = null, updated_at = current_timestamp
                where id = ? and push_token = ?
                """, deviceId, pushToken);
    }

    public record Delivery(UUID id, UUID alertId, UUID deviceId, int attempts, String pushToken,
                           String title, String message, UUID lotId) {
    }
}
