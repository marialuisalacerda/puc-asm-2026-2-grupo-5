package br.pucminas.plantaopet.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Reserva de um serviço. As regras de transição de status ficam no próprio agregado:
 *
 * <pre>
 * REQUESTED ──accept──▶ ACCEPTED ──cancel──▶ CANCELLED
 *     │  └──reject──▶ REJECTED
 *     └──cancel──▶ CANCELLED
 * </pre>
 */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    private UUID id;

    @Column(name = "tutor_id", nullable = false, updatable = false)
    private UUID tutorId;

    @Column(name = "pet_id", nullable = false, updatable = false)
    private UUID petId;

    @Column(name = "caregiver_id", nullable = false, updatable = false)
    private UUID caregiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 30, updatable = false)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status;

    @Column(name = "scheduled_start", nullable = false, updatable = false)
    private Instant scheduledStart;

    @Column(name = "scheduled_end", nullable = false, updatable = false)
    private Instant scheduledEnd;

    @Column(name = "price_cents", nullable = false, updatable = false)
    private long priceCents;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Booking() {
        // JPA
    }

    private Booking(UUID id, UUID tutorId, UUID petId, UUID caregiverId, ServiceType serviceType,
                    Instant scheduledStart, Instant scheduledEnd, long priceCents, Instant now) {
        this.id = Objects.requireNonNull(id);
        this.tutorId = Objects.requireNonNull(tutorId);
        this.petId = Objects.requireNonNull(petId);
        this.caregiverId = Objects.requireNonNull(caregiverId);
        this.serviceType = Objects.requireNonNull(serviceType);
        this.scheduledStart = Objects.requireNonNull(scheduledStart);
        this.scheduledEnd = Objects.requireNonNull(scheduledEnd);
        if (!scheduledEnd.isAfter(scheduledStart)) {
            throw new IllegalArgumentException("scheduledEnd must be after scheduledStart");
        }
        if (priceCents < 0) {
            throw new IllegalArgumentException("priceCents must be >= 0");
        }
        this.priceCents = priceCents;
        this.status = BookingStatus.REQUESTED;
        this.createdAt = Objects.requireNonNull(now);
        this.updatedAt = now;
    }

    /** Cria uma reserva no estado inicial {@link BookingStatus#REQUESTED}. */
    public static Booking request(UUID id, UUID tutorId, UUID petId, UUID caregiverId, ServiceType serviceType,
                                  Instant scheduledStart, Instant scheduledEnd, long priceCents, Instant now) {
        return new Booking(id, tutorId, petId, caregiverId, serviceType, scheduledStart, scheduledEnd, priceCents, now);
    }

    public void accept(Instant now) {
        transitionTo(BookingStatus.ACCEPTED, now);
    }

    public void reject(Instant now) {
        transitionTo(BookingStatus.REJECTED, now);
    }

    public void cancel(Instant now) {
        transitionTo(BookingStatus.CANCELLED, now);
    }

    private void transitionTo(BookingStatus target, Instant now) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(id, status, target);
        }
        this.status = target;
        this.updatedAt = Objects.requireNonNull(now);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTutorId() {
        return tutorId;
    }

    public UUID getPetId() {
        return petId;
    }

    public UUID getCaregiverId() {
        return caregiverId;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getScheduledStart() {
        return scheduledStart;
    }

    public Instant getScheduledEnd() {
        return scheduledEnd;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
