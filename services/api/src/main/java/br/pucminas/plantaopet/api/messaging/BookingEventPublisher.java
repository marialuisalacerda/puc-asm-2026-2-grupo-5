package br.pucminas.plantaopet.api.messaging;

import br.pucminas.plantaopet.api.domain.event.BookingDomainEvent;

/**
 * Porta de saída para publicação de eventos de reserva. A implementação padrão usa RabbitMQ;
 * testes e ambientes sem broker podem fornecer outra implementação.
 */
public interface BookingEventPublisher {

    void publish(BookingDomainEvent event);
}
