package br.pucminas.plantaopet.notification.service.handler;

import br.pucminas.plantaopet.notification.domain.RecipientType;
import br.pucminas.plantaopet.notification.messaging.BookingEventData;
import br.pucminas.plantaopet.notification.messaging.BookingEventEnvelope;
import org.springframework.stereotype.Component;

/** Nova solicitação: avisa o cuidador para aceitar ou recusar. */
@Component
public class BookingCreatedHandler implements BookingEventHandler {

    public static final String EVENT_TYPE = "BookingCreated";

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public NotificationDraft handle(BookingEventEnvelope event) {
        BookingEventData data = event.data();
        String message = "Nova solicitação de " + BookingMessageFormatter.service(data)
                + " para " + BookingMessageFormatter.period(data)
                + " (" + BookingMessageFormatter.price(data) + "). Aceite ou recuse no app.";
        return new NotificationDraft(RecipientType.CAREGIVER, data.caregiverId(), message);
    }
}
