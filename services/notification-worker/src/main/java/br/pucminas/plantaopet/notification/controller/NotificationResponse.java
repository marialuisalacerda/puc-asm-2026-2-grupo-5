package br.pucminas.plantaopet.notification.controller;

import br.pucminas.plantaopet.notification.domain.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID eventId,
        UUID bookingId,
        String recipientType,
        UUID recipientId,
        String channel,
        String message,
        Instant createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getEventId(), n.getBookingId(), n.getRecipientType().name(),
                n.getRecipientId(), n.getChannel().name(), n.getMessage(), n.getCreatedAt());
    }
}
