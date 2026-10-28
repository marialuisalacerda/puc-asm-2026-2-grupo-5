package br.pucminas.plantaopet.notification.service.sender;

import br.pucminas.plantaopet.notification.domain.Notification;
import br.pucminas.plantaopet.notification.domain.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Envio simulado: uma linha de log estruturado por notificação. */
@Component
public class LogNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.LOG;
    }

    @Override
    public void send(Notification notification) {
        log.info("Notification sent channel={} recipient={}:{} bookingId={} eventId={} message=\"{}\"",
                notification.getChannel(),
                notification.getRecipientType(),
                notification.getRecipientId(),
                notification.getBookingId(),
                notification.getEventId(),
                notification.getMessage());
    }
}
