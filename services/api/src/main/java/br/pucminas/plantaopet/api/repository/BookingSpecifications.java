package br.pucminas.plantaopet.api.repository;

import br.pucminas.plantaopet.api.domain.Booking;
import br.pucminas.plantaopet.api.domain.BookingStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/** Filtros opcionais de GET /bookings, combinados somente quando informados. */
public final class BookingSpecifications {

    private BookingSpecifications() {
    }

    public static Specification<Booking> withFilters(UUID tutorId, UUID caregiverId, BookingStatus status) {
        Specification<Booking> spec = Specification.where(null);
        if (tutorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tutorId"), tutorId));
        }
        if (caregiverId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("caregiverId"), caregiverId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return spec;
    }
}
