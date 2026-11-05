package br.pucminas.plantaopet.api.messaging;

import br.pucminas.plantaopet.api.domain.event.BookingDomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Encaminha o evento de domínio ao broker somente após o commit da transação da reserva.
 * Se a transação sofrer rollback, nada é publicado.
 *
 * <p>Limitação conhecida: se o broker estiver indisponível no momento do envio, o evento é
 * registrado em log de erro e não é reenviado (a reserva já está gravada). A evolução natural
 * é um Transactional Outbox, a ser decidido em ADR.
 */
@Component
public class BookingEventRelay {

    private static final Logger log = LoggerFactory.getLogger(BookingEventRelay.class);

    private final BookingEventPublisher publisher;

    public BookingEventRelay(BookingEventPublisher publisher) {
        this.publisher = publisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingEvent(BookingDomainEvent event) {
        try {
            publisher.publish(event);
        } catch (RuntimeException e) {
            log.error("Booking event publish failed eventId={} eventType={} bookingId={}",
                    event.eventId(), event.type().eventName(), event.bookingId(), e);
        }
    }
}
