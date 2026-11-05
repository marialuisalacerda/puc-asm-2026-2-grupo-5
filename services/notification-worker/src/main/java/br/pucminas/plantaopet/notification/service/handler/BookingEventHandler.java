package br.pucminas.plantaopet.notification.service.handler;

import br.pucminas.plantaopet.notification.messaging.BookingEventEnvelope;

/**
 * Strategy por tipo de evento: cada implementação decide o destinatário e o texto da notificação.
 * Para suportar um evento novo basta criar outro {@code @Component} que implemente esta interface.
 */
public interface BookingEventHandler {

    /** Valor de {@code eventType} atendido (ex.: {@code BookingCreated}). */
    String eventType();

    NotificationDraft handle(BookingEventEnvelope event);
}
