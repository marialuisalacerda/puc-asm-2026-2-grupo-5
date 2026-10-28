package br.pucminas.plantaopet.api.controller;

import br.pucminas.plantaopet.api.controller.dto.BookingResponse;
import br.pucminas.plantaopet.api.controller.dto.CreateBookingRequest;
import br.pucminas.plantaopet.api.controller.dto.PriceQuoteResponse;
import br.pucminas.plantaopet.api.controller.dto.ReasonRequest;
import br.pucminas.plantaopet.api.domain.Booking;
import br.pucminas.plantaopet.api.domain.BookingStatus;
import br.pucminas.plantaopet.api.domain.ServiceType;
import br.pucminas.plantaopet.api.service.BookingService;
import br.pucminas.plantaopet.api.service.CreateBookingCommand;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/quote")
    public PriceQuoteResponse quote(
            @RequestParam ServiceType serviceType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant scheduledStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant scheduledEnd) {
        return PriceQuoteResponse.from(bookingService.quote(serviceType, scheduledStart, scheduledEnd));
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        Booking booking = bookingService.create(new CreateBookingCommand(
                request.tutorId(), request.petId(), request.caregiverId(), request.serviceType(),
                request.scheduledStart(), request.scheduledEnd()));
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(booking.getId()).toUri())
                .body(BookingResponse.from(booking));
    }

    @GetMapping
    public List<BookingResponse> list(@RequestParam(required = false) UUID tutorId,
                                      @RequestParam(required = false) UUID caregiverId,
                                      @RequestParam(required = false) BookingStatus status) {
        return bookingService.list(tutorId, caregiverId, status).stream().map(BookingResponse::from).toList();
    }

    @GetMapping("/{bookingId}")
    public BookingResponse get(@PathVariable UUID bookingId) {
        return BookingResponse.from(bookingService.get(bookingId));
    }

    @PatchMapping("/{bookingId}/accept")
    public BookingResponse accept(@PathVariable UUID bookingId) {
        return BookingResponse.from(bookingService.accept(bookingId));
    }

    @PatchMapping("/{bookingId}/reject")
    public BookingResponse reject(@PathVariable UUID bookingId,
                                  @Valid @RequestBody(required = false) ReasonRequest request) {
        return BookingResponse.from(bookingService.reject(bookingId, reasonOf(request)));
    }

    @PatchMapping("/{bookingId}/cancel")
    public BookingResponse cancel(@PathVariable UUID bookingId,
                                  @Valid @RequestBody(required = false) ReasonRequest request) {
        return BookingResponse.from(bookingService.cancel(bookingId, reasonOf(request)));
    }

    private static String reasonOf(ReasonRequest request) {
        return request == null ? null : request.reason();
    }
}
