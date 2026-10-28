package br.pucminas.plantaopet.notification.service.handler;

import br.pucminas.plantaopet.notification.domain.RecipientType;
import br.pucminas.plantaopet.notification.messaging.BookingEventData;
import br.pucminas.plantaopet.notification.messaging.BookingEventEnvelope;
import org.springframework.stereotype.Component;

/**
 * Reserva cancelada: avisa o cuidador, que libera a agenda.
 * O contrato não informa quem cancelou; pela regra da equipe o cancelamento parte do tutor.
 */
@Component
public class BookingCancelledHandler implements BookingEventHandler {

    public static final String EVENT_TYPE = "BookingCancelled";

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public NotificationDraft handle(BookingEventEnvelope event) {
        BookingEventData data = event.data();
        String message = "A reserva de " + BookingMessageFormatter.service(data)
                + " para " + BookingMessageFormatter.period(data) + " foi cancelada."
                + BookingMessageFormatter.reasonSuffix(data);
        return new NotificationDraft(RecipientType.CAREGIVER, data.caregiverId(), message);
    }
}
