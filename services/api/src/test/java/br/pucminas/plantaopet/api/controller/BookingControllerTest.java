package br.pucminas.plantaopet.api.controller;

import br.pucminas.plantaopet.api.domain.Booking;
import br.pucminas.plantaopet.api.domain.BookingStatus;
import br.pucminas.plantaopet.api.domain.InvalidStatusTransitionException;
import br.pucminas.plantaopet.api.domain.ServiceType;
import br.pucminas.plantaopet.api.exception.BusinessRuleException;
import br.pucminas.plantaopet.api.exception.ConflictException;
import br.pucminas.plantaopet.api.exception.InvalidRequestException;
import br.pucminas.plantaopet.api.exception.ResourceNotFoundException;
import br.pucminas.plantaopet.api.service.BookingService;
import br.pucminas.plantaopet.api.service.CreateBookingCommand;
import br.pucminas.plantaopet.api.service.PriceQuote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final UUID TUTOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PET_ID = UUID.fromString("22222222-2222-2222-2222-222222222221");
    private static final UUID CAREGIVER_ID = UUID.fromString("33333333-3333-3333-3333-333333333331");
    private static final UUID BOOKING_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant START = Instant.parse("2026-11-02T13:00:00Z");
    private static final Instant END = Instant.parse("2026-11-02T15:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    private static final String VALID_BODY = """
            {
              "tutorId": "11111111-1111-1111-1111-111111111111",
              "petId": "22222222-2222-2222-2222-222222222221",
              "caregiverId": "33333333-3333-3333-3333-333333333331",
              "serviceType": "WALK",
              "scheduledStart": "2026-11-02T13:00:00Z",
              "scheduledEnd": "2026-11-02T15:00:00Z"
            }
            """;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BookingService bookingService;

    private static Booking booking() {
        return Booking.request(BOOKING_ID, TUTOR_ID, PET_ID, CAREGIVER_ID, ServiceType.WALK, START, END, 6000, NOW);
    }

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        when(bookingService.create(any(CreateBookingCommand.class))).thenReturn(booking());

        mvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/bookings/" + BOOKING_ID)))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(BOOKING_ID.toString()))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.serviceType").value("WALK"))
                .andExpect(jsonPath("$.priceCents").value(6000))
                .andExpect(jsonPath("$.scheduledStart").value("2026-11-02T13:00:00Z"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-28T12:00:00Z"));

        verify(bookingService).create(new CreateBookingCommand(TUTOR_ID, PET_ID, CAREGIVER_ID, ServiceType.WALK,
                START, END));
    }

    @Test
    void missingFieldsReturn400ProblemWithErrors() throws Exception {
        mvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON).content("{\"serviceType\":\"WALK\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.instance").value("/bookings"))
                .andExpect(jsonPath("$.errors", hasSize(5)))
                .andExpect(jsonPath("$.errors[*].field", hasItem("tutorId")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("scheduledEnd")))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
        verifyNoInteractions(bookingService);
    }

    @Test
    void invalidEnumReturns400Problem() throws Exception {
        mvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"WALK\"", "\"SWIM\"")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void endBeforeStartReturns400WithFieldError() throws Exception {
        when(bookingService.create(any())).thenThrow(
                new InvalidRequestException("scheduledEnd", "scheduledEnd must be after scheduledStart"));

        mvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("scheduledEnd"));
    }

    @Test
    void businessRuleViolationReturns422() throws Exception {
        when(bookingService.create(any())).thenThrow(new BusinessRuleException("Caregiver is not active"));

        mvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value("Caregiver is not active"))
                .andExpect(jsonPath("$.type").value("https://plantaopet.local/problems/business-rule"));
    }

    @Test
    void overlapReturns409() throws Exception {
        when(bookingService.create(any())).thenThrow(new ConflictException("Caregiver already has a booking"));

        mvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void unknownReferenceReturns404() throws Exception {
        when(bookingService.create(any())).thenThrow(new ResourceNotFoundException("Caregiver", CAREGIVER_ID));

        mvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getUnknownBookingReturns404() throws Exception {
        when(bookingService.get(BOOKING_ID)).thenThrow(new ResourceNotFoundException("Booking", BOOKING_ID));

        mvc.perform(get("/bookings/{id}", BOOKING_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.instance").value("/bookings/" + BOOKING_ID));
    }

    @Test
    void invalidUuidReturns400() throws Exception {
        mvc.perform(get("/bookings/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void listPassesFilters() throws Exception {
        when(bookingService.list(TUTOR_ID, null, BookingStatus.REQUESTED)).thenReturn(List.of(booking()));

        mvc.perform(get("/bookings").param("tutorId", TUTOR_ID.toString()).param("status", "REQUESTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(BOOKING_ID.toString()));
    }

    @Test
    void acceptReturns200() throws Exception {
        Booking accepted = booking();
        accepted.accept(NOW);
        when(bookingService.accept(BOOKING_ID)).thenReturn(accepted);

        mvc.perform(patch("/bookings/{id}/accept", BOOKING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void invalidTransitionReturns409() throws Exception {
        when(bookingService.accept(BOOKING_ID)).thenThrow(
                new InvalidStatusTransitionException(BOOKING_ID, BookingStatus.CANCELLED, BookingStatus.ACCEPTED));

        mvc.perform(patch("/bookings/{id}/accept", BOOKING_ID))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://plantaopet.local/problems/invalid-status-transition"));
    }

    @Test
    void rejectPassesReason() throws Exception {
        Booking rejected = booking();
        rejected.reject(NOW);
        when(bookingService.reject(BOOKING_ID, "agenda cheia")).thenReturn(rejected);

        mvc.perform(patch("/bookings/{id}/reject", BOOKING_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"agenda cheia\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void cancelWithoutBodyIsAllowed() throws Exception {
        Booking cancelled = booking();
        cancelled.cancel(NOW);
        when(bookingService.cancel(eq(BOOKING_ID), isNull())).thenReturn(cancelled);

        mvc.perform(patch("/bookings/{id}/cancel", BOOKING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void reasonLongerThan300Returns400() throws Exception {
        String reason = "x".repeat(301);

        mvc.perform(patch("/bookings/{id}/cancel", BOOKING_ID)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
        verifyNoInteractions(bookingService);
    }

    @Test
    void quoteReturnsPrice() throws Exception {
        Instant end = Instant.parse("2026-11-02T14:30:00Z");
        when(bookingService.quote(ServiceType.WALK, START, end))
                .thenReturn(new PriceQuote(ServiceType.WALK, START, end, 6000, "BRL"));

        mvc.perform(get("/bookings/quote")
                        .param("serviceType", "WALK")
                        .param("scheduledStart", "2026-11-02T13:00:00Z")
                        .param("scheduledEnd", "2026-11-02T14:30:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceCents").value(6000))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.serviceType").value("WALK"));
    }

    @Test
    void quoteWithoutParamsReturns400() throws Exception {
        mvc.perform(get("/bookings/quote").param("serviceType", "WALK"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void corsAllowsExpoWebOrigin() throws Exception {
        mvc.perform(options("/bookings")
                        .header(HttpHeaders.ORIGIN, "http://localhost:8081")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8081"));
    }

    @Test
    void corsRejectsUnknownOrigin() throws Exception {
        mvc.perform(options("/bookings")
                        .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}
