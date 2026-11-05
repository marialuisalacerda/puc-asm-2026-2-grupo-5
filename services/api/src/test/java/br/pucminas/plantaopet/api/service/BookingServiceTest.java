package br.pucminas.plantaopet.api.service;

import br.pucminas.plantaopet.api.domain.Booking;
import br.pucminas.plantaopet.api.domain.BookingStatus;
import br.pucminas.plantaopet.api.domain.Caregiver;
import br.pucminas.plantaopet.api.domain.InvalidStatusTransitionException;
import br.pucminas.plantaopet.api.domain.Pet;
import br.pucminas.plantaopet.api.domain.PetSize;
import br.pucminas.plantaopet.api.domain.ServiceType;
import br.pucminas.plantaopet.api.domain.event.BookingDomainEvent;
import br.pucminas.plantaopet.api.domain.event.BookingEventType;
import br.pucminas.plantaopet.api.exception.BusinessRuleException;
import br.pucminas.plantaopet.api.exception.ConflictException;
import br.pucminas.plantaopet.api.exception.InvalidRequestException;
import br.pucminas.plantaopet.api.exception.ResourceNotFoundException;
import br.pucminas.plantaopet.api.pricing.CompanionshipPriceStrategy;
import br.pucminas.plantaopet.api.pricing.HomeVisitPriceStrategy;
import br.pucminas.plantaopet.api.pricing.HostingPriceStrategy;
import br.pucminas.plantaopet.api.pricing.PriceCalculator;
import br.pucminas.plantaopet.api.pricing.TemporaryCarePriceStrategy;
import br.pucminas.plantaopet.api.pricing.WalkPriceStrategy;
import br.pucminas.plantaopet.api.repository.BookingRepository;
import br.pucminas.plantaopet.api.repository.CaregiverRepository;
import br.pucminas.plantaopet.api.repository.PetRepository;
import br.pucminas.plantaopet.api.repository.TutorRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final Instant START = Instant.parse("2026-11-02T13:00:00Z");
    private static final Instant END = Instant.parse("2026-11-02T15:00:00Z");

    private static final UUID TUTOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_TUTOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111112");
    private static final UUID PET_ID = UUID.fromString("22222222-2222-2222-2222-222222222221");
    private static final UUID CAREGIVER_ID = UUID.fromString("33333333-3333-3333-3333-333333333331");

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private CaregiverRepository caregiverRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SimpleMeterRegistry meterRegistry;
    private BookingService service;
    private final AtomicInteger idSequence = new AtomicInteger();

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        PriceCalculator priceCalculator = new PriceCalculator(List.of(
                new WalkPriceStrategy(), new CompanionshipPriceStrategy(), new HomeVisitPriceStrategy(),
                new HostingPriceStrategy(), new TemporaryCarePriceStrategy()));
        IdGenerator ids = () -> new UUID(0xABCDL, idSequence.incrementAndGet());
        service = new BookingService(bookingRepository, tutorRepository, petRepository, caregiverRepository,
                priceCalculator, eventPublisher, meterRegistry, ids, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static CreateBookingCommand command(ServiceType type, Instant start, Instant end) {
        return new CreateBookingCommand(TUTOR_ID, PET_ID, CAREGIVER_ID, type, start, end);
    }

    private static Pet pet(UUID ownerId) {
        return new Pet(PET_ID, ownerId, "Thor", "DOG", PetSize.LARGE, null, NOW);
    }

    private static Caregiver caregiver(boolean active) {
        return new Caregiver(CAREGIVER_ID, "Carla Cuidadora", "carla@example.com", active, NOW);
    }

    private void givenValidReferences() {
        when(tutorRepository.existsById(TUTOR_ID)).thenReturn(true);
        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet(TUTOR_ID)));
        when(caregiverRepository.findByIdForUpdate(CAREGIVER_ID)).thenReturn(Optional.of(caregiver(true)));
    }

    private static Booking existingBooking() {
        return Booking.request(UUID.fromString("44444444-4444-4444-4444-444444444444"), TUTOR_ID, PET_ID,
                CAREGIVER_ID, ServiceType.WALK, START, END, 6000, NOW.minusSeconds(3600));
    }

    private BookingDomainEvent capturedEvent() {
        ArgumentCaptor<BookingDomainEvent> captor = ArgumentCaptor.forClass(BookingDomainEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    @Nested
    class Create {

        @Test
        void createsRequestedBookingWithCalculatedPrice() {
            givenValidReferences();
            when(bookingRepository.existsOverlapping(eq(CAREGIVER_ID), anyCollection(), eq(START), eq(END)))
                    .thenReturn(false);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Booking booking = service.create(command(ServiceType.WALK, START, END));

            assertThat(booking.getId()).isNotNull();
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.REQUESTED);
            assertThat(booking.getPriceCents()).isEqualTo(6000);
            assertThat(booking.getTutorId()).isEqualTo(TUTOR_ID);
            assertThat(booking.getCaregiverId()).isEqualTo(CAREGIVER_ID);
            assertThat(booking.getCreatedAt()).isEqualTo(NOW);
            verify(bookingRepository).existsOverlapping(CAREGIVER_ID, BookingStatus.BLOCKING, START, END);
            assertThat(meterRegistry.counter("booking.created", "service_type", "WALK").count()).isEqualTo(1.0);
        }

        @Test
        void publishesBookingCreatedEvent() {
            givenValidReferences();
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Booking booking = service.create(command(ServiceType.HOSTING,
                    Instant.parse("2026-11-02T10:00:00Z"), Instant.parse("2026-11-03T16:00:00Z")));

            BookingDomainEvent event = capturedEvent();
            assertThat(event.type()).isEqualTo(BookingEventType.CREATED);
            assertThat(event.eventId()).isNotNull().isNotEqualTo(booking.getId());
            assertThat(event.occurredAt()).isEqualTo(NOW);
            assertThat(event.bookingId()).isEqualTo(booking.getId());
            assertThat(event.status()).isEqualTo(BookingStatus.REQUESTED);
            assertThat(event.serviceType()).isEqualTo(ServiceType.HOSTING);
            assertThat(event.priceCents()).isEqualTo(24000);
            assertThat(event.reason()).isNull();
        }

        @Test
        void rejectsPetOfAnotherTutorWith422() {
            when(tutorRepository.existsById(TUTOR_ID)).thenReturn(true);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet(OTHER_TUTOR_ID)));

            assertThatThrownBy(() -> service.create(command(ServiceType.WALK, START, END)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("does not belong");
            verify(bookingRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        void rejectsInactiveCaregiverWith422() {
            when(tutorRepository.existsById(TUTOR_ID)).thenReturn(true);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet(TUTOR_ID)));
            when(caregiverRepository.findByIdForUpdate(CAREGIVER_ID)).thenReturn(Optional.of(caregiver(false)));

            assertThatThrownBy(() -> service.create(command(ServiceType.WALK, START, END)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not active");
            verify(bookingRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        void rejectsStartInThePastWith422() {
            Instant past = NOW.minusSeconds(60);

            assertThatThrownBy(() -> service.create(command(ServiceType.WALK, past, past.plusSeconds(3600))))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("future");
            verifyNoInteractions(bookingRepository, eventPublisher);
        }

        @Test
        void rejectsEndNotAfterStartWith400() {
            assertThatThrownBy(() -> service.create(command(ServiceType.WALK, START, START)))
                    .isInstanceOf(InvalidRequestException.class)
                    .extracting("field").isEqualTo("scheduledEnd");
            verifyNoInteractions(bookingRepository, eventPublisher);
        }

        @Test
        void rejectsOverlappingBookingWith409() {
            givenValidReferences();
            when(bookingRepository.existsOverlapping(eq(CAREGIVER_ID), anyCollection(), eq(START), eq(END)))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.create(command(ServiceType.WALK, START, END)))
                    .isInstanceOf(ConflictException.class);
            verify(bookingRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
            assertThat(meterRegistry.find("booking.created").counter()).isNull();
        }

        @Test
        void unknownCaregiverIs404() {
            when(tutorRepository.existsById(TUTOR_ID)).thenReturn(true);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet(TUTOR_ID)));
            when(caregiverRepository.findByIdForUpdate(CAREGIVER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(command(ServiceType.WALK, START, END)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Caregiver");
        }

        @Test
        void unknownTutorIs404() {
            when(tutorRepository.existsById(TUTOR_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.create(command(ServiceType.WALK, START, END)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Tutor");
        }
    }

    @Nested
    class Transitions {

        private Booking booking;

        @BeforeEach
        void givenBooking() {
            booking = existingBooking();
            when(bookingRepository.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));
        }

        private void savesAnything() {
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void acceptPublishesBookingAccepted() {
            savesAnything();

            Booking result = service.accept(booking.getId());

            assertThat(result.getStatus()).isEqualTo(BookingStatus.ACCEPTED);
            assertThat(result.getUpdatedAt()).isEqualTo(NOW);
            BookingDomainEvent event = capturedEvent();
            assertThat(event.type()).isEqualTo(BookingEventType.ACCEPTED);
            assertThat(event.status()).isEqualTo(BookingStatus.ACCEPTED);
            assertThat(event.bookingId()).isEqualTo(booking.getId());
            assertThat(meterRegistry.counter("booking.accepted", "service_type", "WALK").count()).isEqualTo(1.0);
        }

        @Test
        void rejectPublishesBookingRejectedWithReason() {
            savesAnything();

            Booking result = service.reject(booking.getId(), "  agenda cheia  ");

            assertThat(result.getStatus()).isEqualTo(BookingStatus.REJECTED);
            BookingDomainEvent event = capturedEvent();
            assertThat(event.type()).isEqualTo(BookingEventType.REJECTED);
            assertThat(event.reason()).isEqualTo("agenda cheia");
            assertThat(meterRegistry.counter("booking.rejected", "service_type", "WALK").count()).isEqualTo(1.0);
        }

        @Test
        void cancelAcceptedBookingPublishesBookingCancelled() {
            savesAnything();
            booking.accept(NOW);

            Booking result = service.cancel(booking.getId(), null);

            assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            BookingDomainEvent event = capturedEvent();
            assertThat(event.type()).isEqualTo(BookingEventType.CANCELLED);
            assertThat(event.reason()).isNull();
            assertThat(meterRegistry.counter("booking.cancelled", "service_type", "WALK").count()).isEqualTo(1.0);
        }

        @Test
        void rejectAcceptedBookingIs409AndPublishesNothing() {
            booking.accept(NOW);

            assertThatThrownBy(() -> service.reject(booking.getId(), null))
                    .isInstanceOf(InvalidStatusTransitionException.class);
            verify(bookingRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        void acceptCancelledBookingIs409() {
            booking.cancel(NOW);

            assertThatThrownBy(() -> service.accept(booking.getId()))
                    .isInstanceOf(InvalidStatusTransitionException.class);
            verifyNoInteractions(eventPublisher);
        }
    }

    @Test
    void transitionOfUnknownBookingIs404() {
        UUID id = UUID.randomUUID();
        when(bookingRepository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.accept(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void quoteUsesStrategyAndBrl() {
        PriceQuote quote = service.quote(ServiceType.WALK, START, Instant.parse("2026-11-02T14:30:00Z"));

        assertThat(quote.priceCents()).isEqualTo(6000);
        assertThat(quote.currency()).isEqualTo("BRL");
    }

    @Test
    void quoteRejectsInvalidPeriod() {
        assertThatThrownBy(() -> service.quote(ServiceType.WALK, END, START))
                .isInstanceOf(InvalidRequestException.class);
    }
}
