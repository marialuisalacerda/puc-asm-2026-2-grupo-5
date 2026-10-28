package br.pucminas.plantaopet.api.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    private static Booking newBooking() {
        return Booking.request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ServiceType.WALK, NOW.plusSeconds(3600), NOW.plusSeconds(7200), 3000, NOW);
    }

    @Test
    void startsAsRequested() {
        Booking booking = newBooking();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REQUESTED);
        assertThat(booking.getCreatedAt()).isEqualTo(NOW);
        assertThat(booking.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void acceptThenCancel() {
        Booking booking = newBooking();
        Instant later = NOW.plusSeconds(60);

        booking.accept(later);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACCEPTED);
        assertThat(booking.getUpdatedAt()).isEqualTo(later);

        booking.cancel(later.plusSeconds(60));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void rejectFromRequested() {
        Booking booking = newBooking();

        booking.reject(NOW);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void cancelFromRequested() {
        Booking booking = newBooking();

        booking.cancel(NOW);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cannotRejectAcceptedBooking() {
        Booking booking = newBooking();
        booking.accept(NOW);

        assertThatThrownBy(() -> booking.reject(NOW))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("ACCEPTED to REJECTED");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACCEPTED);
    }

    @Test
    void finalStatesDoNotTransition() {
        Booking rejected = newBooking();
        rejected.reject(NOW);
        Booking cancelled = newBooking();
        cancelled.cancel(NOW);

        assertThatThrownBy(() -> rejected.accept(NOW)).isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> rejected.cancel(NOW)).isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> cancelled.accept(NOW)).isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> cancelled.cancel(NOW)).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void periodMustBePositive() {
        assertThatThrownBy(() -> Booking.request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), ServiceType.WALK, NOW, NOW, 0, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
