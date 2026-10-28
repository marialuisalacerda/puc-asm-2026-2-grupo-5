package br.pucminas.plantaopet.notification.controller;

import br.pucminas.plantaopet.notification.repository.NotificationRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Consulta read-only para demonstração: lista as notificações do banco do próprio worker.
 * Sem {@code bookingId}, retorna as 50 mais recentes.
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<NotificationResponse> list(@RequestParam(required = false) UUID bookingId) {
        var notifications = bookingId == null
                ? notificationRepository.findTop50ByOrderByCreatedAtDesc()
                : notificationRepository.findByBookingIdOrderByCreatedAtAsc(bookingId);
        return notifications.stream().map(NotificationResponse::from).toList();
    }
}
