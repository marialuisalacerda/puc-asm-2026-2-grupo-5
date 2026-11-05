package br.pucminas.plantaopet.notification.service.handler;

import br.pucminas.plantaopet.notification.domain.RecipientType;

import java.util.Objects;
import java.util.UUID;

/** Decisão de um handler: para quem notificar e com qual texto. */
public record NotificationDraft(RecipientType recipientType, UUID recipientId, String message) {

    public NotificationDraft {
        Objects.requireNonNull(recipientType, "recipientType");
        Objects.requireNonNull(recipientId, "recipientId");
        Objects.requireNonNull(message, "message");
    }
}
