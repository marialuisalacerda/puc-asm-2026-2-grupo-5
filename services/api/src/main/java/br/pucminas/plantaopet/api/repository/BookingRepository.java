package br.pucminas.plantaopet.api.repository;

import br.pucminas.plantaopet.api.domain.Booking;
import br.pucminas.plantaopet.api.domain.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {

    /**
     * Existe reserva do cuidador, em um dos status informados, que intersecta o período
     * [start, end)? Períodos encostados (fim de uma = início da outra) não conflitam.
     * Atendida pelo índice idx_bookings_caregiver_status_start (V5).
     */
    @Query("""
            select count(b) > 0 from Booking b
            where b.caregiverId = :caregiverId
              and b.status in :statuses
              and b.scheduledStart < :end
              and b.scheduledEnd > :start
            """)
    boolean existsOverlapping(@Param("caregiverId") UUID caregiverId,
                              @Param("statuses") Collection<BookingStatus> statuses,
                              @Param("start") Instant start,
                              @Param("end") Instant end);

    /** Carrega a reserva com lock de escrita para transições de status concorrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") UUID id);
}
