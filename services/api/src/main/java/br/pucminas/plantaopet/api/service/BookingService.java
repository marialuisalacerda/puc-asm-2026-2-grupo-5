package br.pucminas.plantaopet.api.service;

import br.pucminas.plantaopet.api.domain.Booking;
import br.pucminas.plantaopet.api.domain.BookingStatus;
import br.pucminas.plantaopet.api.domain.Caregiver;
import br.pucminas.plantaopet.api.domain.Pet;
import br.pucminas.plantaopet.api.domain.ServiceType;
import br.pucminas.plantaopet.api.domain.event.BookingDomainEvent;
import br.pucminas.plantaopet.api.domain.event.BookingEventType;
import br.pucminas.plantaopet.api.exception.BusinessRuleException;
import br.pucminas.plantaopet.api.exception.ConflictException;
import br.pucminas.plantaopet.api.exception.InvalidRequestException;
import br.pucminas.plantaopet.api.exception.ResourceNotFoundException;
import br.pucminas.plantaopet.api.pricing.PriceCalculator;
import br.pucminas.plantaopet.api.repository.BookingRepository;
import br.pucminas.plantaopet.api.repository.BookingSpecifications;
import br.pucminas.plantaopet.api.repository.CaregiverRepository;
import br.pucminas.plantaopet.api.repository.PetRepository;
import br.pucminas.plantaopet.api.repository.TutorRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Casos de uso da reserva. Orquestra validações, cálculo de preço (Strategy), persistência
 * (Repository) e emissão do evento de domínio. A publicação no RabbitMQ acontece apenas após
 * o commit, em {@code messaging.BookingEventRelay}.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final TutorRepository tutorRepository;
    private final PetRepository petRepository;
    private final CaregiverRepository caregiverRepository;
    private final PriceCalculator priceCalculator;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository,
                          TutorRepository tutorRepository,
                          PetRepository petRepository,
                          CaregiverRepository caregiverRepository,
                          PriceCalculator priceCalculator,
                          ApplicationEventPublisher eventPublisher,
                          MeterRegistry meterRegistry,
                          IdGenerator idGenerator,
                          Clock clock) {
        this.bookingRepository = bookingRepository;
        this.tutorRepository = tutorRepository;
        this.petRepository = petRepository;
        this.caregiverRepository = caregiverRepository;
        this.priceCalculator = priceCalculator;
        this.eventPublisher = eventPublisher;
        this.meterRegistry = meterRegistry;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public PriceQuote quote(ServiceType serviceType, Instant start, Instant end) {
        requireValidPeriod(start, end);
        long price = priceCalculator.calculate(serviceType, start, end);
        return new PriceQuote(serviceType, start, end, price, PriceCalculator.CURRENCY);
    }

    @Transactional
    public Booking create(CreateBookingCommand command) {
        Instant start = command.scheduledStart();
        Instant end = command.scheduledEnd();
        requireValidPeriod(start, end);

        Instant now = now();
        if (!start.isAfter(now)) {
            throw new BusinessRuleException("scheduledStart must be in the future");
        }

        if (!tutorRepository.existsById(command.tutorId())) {
            throw new ResourceNotFoundException("Tutor", command.tutorId());
        }
        Pet pet = petRepository.findById(command.petId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet", command.petId()));
        if (!pet.belongsTo(command.tutorId())) {
            throw new BusinessRuleException("Pet %s does not belong to tutor %s"
                    .formatted(command.petId(), command.tutorId()));
        }

        // Lock no cuidador serializa reservas concorrentes dele até o commit.
        Caregiver caregiver = caregiverRepository.findByIdForUpdate(command.caregiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Caregiver", command.caregiverId()));
        if (!caregiver.isActive()) {
            throw new BusinessRuleException("Caregiver %s is not active".formatted(caregiver.getId()));
        }
        if (bookingRepository.existsOverlapping(caregiver.getId(), BookingStatus.BLOCKING, start, end)) {
            log.info("Booking rejected by overlap caregiverId={} start={} end={}", caregiver.getId(), start, end);
            throw new ConflictException("Caregiver already has a booking in this period");
        }

        long price = priceCalculator.calculate(command.serviceType(), start, end);
        Booking booking = Booking.request(idGenerator.newId(), command.tutorId(), command.petId(),
                caregiver.getId(), command.serviceType(), start, end, price, now);
        booking = bookingRepository.save(booking);

        publish(BookingEventType.CREATED, booking, null, now);
        log.info("Booking created bookingId={} status={} serviceType={} priceCents={}",
                booking.getId(), booking.getStatus(), booking.getServiceType(), booking.getPriceCents());
        return booking;
    }

    @Transactional
    public Booking accept(UUID bookingId) {
        return transition(bookingId, BookingEventType.ACCEPTED, null, Booking::accept);
    }

    @Transactional
    public Booking reject(UUID bookingId, String reason) {
        return transition(bookingId, BookingEventType.REJECTED, reason, Booking::reject);
    }

    @Transactional
    public Booking cancel(UUID bookingId, String reason) {
        return transition(bookingId, BookingEventType.CANCELLED, reason, Booking::cancel);
    }

    @Transactional(readOnly = true)
    public Booking get(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
    }

    @Transactional(readOnly = true)
    public List<Booking> list(UUID tutorId, UUID caregiverId, BookingStatus status) {
        return bookingRepository.findAll(BookingSpecifications.withFilters(tutorId, caregiverId, status),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private Booking transition(UUID bookingId, BookingEventType type, String reason,
                               BiConsumer<Booking, Instant> action) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        BookingStatus previous = booking.getStatus();
        Instant now = now();
        action.accept(booking, now);
        booking = bookingRepository.save(booking);

        String normalizedReason = (reason == null || reason.isBlank()) ? null : reason.trim();
        publish(type, booking, normalizedReason, now);
        log.info("Booking status changed bookingId={} from={} to={}", booking.getId(), previous, booking.getStatus());
        return booking;
    }

    private void publish(BookingEventType type, Booking booking, String reason, Instant now) {
        eventPublisher.publishEvent(BookingDomainEvent.of(type, booking, reason, idGenerator.newId(), now));
        meterRegistry.counter(metricName(type), "service_type", booking.getServiceType().name()).increment();
    }

    private static String metricName(BookingEventType type) {
        return switch (type) {
            case CREATED -> "booking.created";
            case ACCEPTED -> "booking.accepted";
            case REJECTED -> "booking.rejected";
            case CANCELLED -> "booking.cancelled";
        };
    }

    private static void requireValidPeriod(Instant start, Instant end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new InvalidRequestException("scheduledEnd", "scheduledEnd must be after scheduledStart");
        }
    }

    private Instant now() {
        // PostgreSQL guarda microssegundos; truncar mantém resposta e banco idênticos.
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
