package br.pucminas.plantaopet.api.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados da reserva. Todos os valores do CHECK ck_bookings_status existem no enum,
 * mas nesta versão apenas o fluxo REQUESTED → ACCEPTED/REJECTED/CANCELLED é permitido.
 * PAID, IN_PROGRESS, COMPLETED e EXPIRED ficam reservados para evoluções futuras.
 */
public enum BookingStatus {
    REQUESTED,
    ACCEPTED,
    PAID,
    IN_PROGRESS,
    COMPLETED,
    REJECTED,
    CANCELLED,
    EXPIRED;

    /** Estados que ocupam a agenda do cuidador (regra de sobreposição). */
    public static final Set<BookingStatus> BLOCKING = EnumSet.of(REQUESTED, ACCEPTED);

    public boolean canTransitionTo(BookingStatus target) {
        return switch (this) {
            case REQUESTED -> target == ACCEPTED || target == REJECTED || target == CANCELLED;
            case ACCEPTED -> target == CANCELLED;
            default -> false;
        };
    }
}
