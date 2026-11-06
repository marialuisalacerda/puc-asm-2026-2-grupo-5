package br.pucminas.plantaopet.notification.controller;

import br.pucminas.plantaopet.notification.domain.Notification;
import br.pucminas.plantaopet.notification.domain.NotificationChannel;
import br.pucminas.plantaopet.notification.domain.RecipientType;
import br.pucminas.plantaopet.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationRepository notificationRepository;

    @Test
    void listsNotificationsOfABooking() throws Exception {
        UUID bookingId = UUID.randomUUID();
        Notification notification = new Notification(UUID.randomUUID(), UUID.randomUUID(), bookingId,
                RecipientType.CAREGIVER, UUID.randomUUID(), NotificationChannel.LOG, "Nova solicitação",
                Instant.parse("2026-10-28T21:00:00Z"));
        when(notificationRepository.findByBookingIdOrderByCreatedAtAsc(bookingId)).thenReturn(List.of(notification));

        mockMvc.perform(get("/notifications").param("bookingId", bookingId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$[0].recipientType").value("CAREGIVER"))
                .andExpect(jsonPath("$[0].channel").value("LOG"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-10-28T21:00:00Z"));
    }

    @Test
    void withoutBookingIdReturnsLatestNotifications() throws Exception {
        when(notificationRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(List.of());

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void invalidBookingIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/notifications").param("bookingId", "abc"))
                .andExpect(status().isBadRequest());
    }
}
