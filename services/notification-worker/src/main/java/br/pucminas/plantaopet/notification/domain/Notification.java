package br.pucminas.plantaopet.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Notificação gerada a partir de um evento de reserva. Dados de propriedade do worker. */
@Entity
@Table(name = "notifications")
public class Notification implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 20)
    private RecipientType recipientType;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private NotificationChannel channel;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    protected Notification() {
        this.isNew = false;
    }

    public Notification(UUID id, UUID eventId, UUID bookingId, RecipientType recipientType, UUID recipientId,
                        NotificationChannel channel, String message, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.eventId = Objects.requireNonNull(eventId);
        this.bookingId = Objects.requireNonNull(bookingId);
        this.recipientType = Objects.requireNonNull(recipientType);
        this.recipientId = Objects.requireNonNull(recipientId);
        this.channel = Objects.requireNonNull(channel);
        this.message = Objects.requireNonNull(message);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public RecipientType getRecipientType() {
        return recipientType;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
