package br.pucminas.plantaopet.notification.service.handler;

import br.pucminas.plantaopet.notification.domain.RecipientType;
import br.pucminas.plantaopet.notification.messaging.BookingEventData;
import br.pucminas.plantaopet.notification.messaging.BookingEventEnvelope;
import org.springframework.stereotype.Component;

/** Reserva aceita pelo cuidador: avisa o tutor. */
@Component
public class BookingAcceptedHandler implements BookingEventHandler {

    public static final String EVENT_TYPE = "BookingAccepted";

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public NotificationDraft handle(BookingEventEnvelope event) {
        BookingEventData data = event.data();
        String message = "Sua reserva de " + BookingMessageFormatter.service(data)
                + " para " + BookingMessageFormatter.period(data) + " foi aceita pelo cuidador.";
        return new NotificationDraft(RecipientType.TUTOR, data.tutorId(), message);
    }
}
