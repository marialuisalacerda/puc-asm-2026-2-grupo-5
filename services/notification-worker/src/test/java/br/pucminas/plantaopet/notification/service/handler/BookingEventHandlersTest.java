package br.pucminas.plantaopet.notification.service.handler;

import br.pucminas.plantaopet.notification.TestEvents;
import br.pucminas.plantaopet.notification.domain.RecipientType;
import br.pucminas.plantaopet.notification.messaging.BookingEventEnvelope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BookingEventHandlersTest {

    @Test
    void bookingCreatedNotifiesCaregiverWithServicePeriodAndPrice() {
        BookingEventEnvelope event = TestEvents.envelope("BookingCreated");

        NotificationDraft draft = new BookingCreatedHandler().handle(event);

        assertThat(draft.recipientType()).isEqualTo(RecipientType.CAREGIVER);
        assertThat(draft.recipientId()).isEqualTo(TestEvents.CAREGIVER_CARLA);
        // 13:00Z = 10:00 em America/Sao_Paulo
        assertThat(draft.message())
                .contains("passeio")
                .contains("02/11/2026 10:00 a 02/11/2026 12:00")
                .contains("R$ 60,00");
    }

    @Test
    void bookingAcceptedNotifiesTutor() {
        NotificationDraft draft = new BookingAcceptedHandler().handle(TestEvents.envelope("BookingAccepted"));

        assertThat(draft.recipientType()).isEqualTo(RecipientType.TUTOR);
        assertThat(draft.recipientId()).isEqualTo(TestEvents.TUTOR_ANA);
        assertThat(draft.message()).contains("aceita");
    }

    @Test
    void bookingRejectedNotifiesTutorIncludingReason() {
        BookingEventEnvelope event = TestEvents.envelope(UUID.randomUUID(), "BookingRejected", UUID.randomUUID(),
                "Agenda indisponível");

        NotificationDraft draft = new BookingRejectedHandler().handle(event);

        assertThat(draft.recipientType()).isEqualTo(RecipientType.TUTOR);
        assertThat(draft.recipientId()).isEqualTo(TestEvents.TUTOR_ANA);
        assertThat(draft.message()).contains("recusada").endsWith("Motivo: Agenda indisponível");
    }

    @Test
    void bookingRejectedWithoutReasonHasNoReasonSuffix() {
        NotificationDraft draft = new BookingRejectedHandler().handle(TestEvents.envelope("BookingRejected"));

        assertThat(draft.message()).doesNotContain("Motivo");
    }

    @Test
    void bookingCancelledNotifiesCaregiver() {
        BookingEventEnvelope event = TestEvents.envelope(UUID.randomUUID(), "BookingCancelled", UUID.randomUUID(),
                "Viagem adiada");

        NotificationDraft draft = new BookingCancelledHandler().handle(event);

        assertThat(draft.recipientType()).isEqualTo(RecipientType.CAREGIVER);
        assertThat(draft.recipientId()).isEqualTo(TestEvents.CAREGIVER_CARLA);
        assertThat(draft.message()).contains("cancelada").contains("Motivo: Viagem adiada");
    }

    @Test
    void registryResolvesEachContractEventTypeAndRejectsUnknown() {
        BookingEventHandlerRegistry registry = new BookingEventHandlerRegistry(List.of(
                new BookingCreatedHandler(), new BookingAcceptedHandler(),
                new BookingRejectedHandler(), new BookingCancelledHandler()));

        assertThat(registry.find("BookingCreated")).containsInstanceOf(BookingCreatedHandler.class);
        assertThat(registry.find("BookingAccepted")).containsInstanceOf(BookingAcceptedHandler.class);
        assertThat(registry.find("BookingRejected")).containsInstanceOf(BookingRejectedHandler.class);
        assertThat(registry.find("BookingCancelled")).containsInstanceOf(BookingCancelledHandler.class);
        assertThat(registry.find("BookingPaid")).isEmpty();
        assertThat(registry.find(null)).isEmpty();
    }
}
