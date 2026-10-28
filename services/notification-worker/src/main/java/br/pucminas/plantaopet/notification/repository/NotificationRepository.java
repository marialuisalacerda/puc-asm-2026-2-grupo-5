package br.pucminas.plantaopet.notification.repository;

import br.pucminas.plantaopet.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByBookingIdOrderByCreatedAtAsc(UUID bookingId);

    List<Notification> findTop50ByOrderByCreatedAtDesc();

    long countByEventId(UUID eventId);
}
