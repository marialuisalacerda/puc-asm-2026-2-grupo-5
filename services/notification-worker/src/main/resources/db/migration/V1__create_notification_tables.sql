-- notification-worker: banco próprio "notifications" (ownership do worker, ADR-003 / ADR-004).
-- Nenhuma tabela da API é referenciada: booking_id e recipient_id são apenas identificadores
-- recebidos no evento (sem FK física entre bancos).

-- Registro de idempotência: um eventId processado com sucesso aparece exatamente uma vez.
CREATE TABLE processed_events (
    event_id     UUID         PRIMARY KEY,
    event_type   VARCHAR(50)  NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL
);

-- Notificações "enviadas" (envio simulado) a partir dos eventos de reserva.
CREATE TABLE notifications (
    id             UUID          PRIMARY KEY,
    event_id       UUID          NOT NULL,
    booking_id     UUID          NOT NULL,
    recipient_type VARCHAR(20)   NOT NULL,
    recipient_id   UUID          NOT NULL,
    channel        VARCHAR(30)   NOT NULL,
    message        VARCHAR(1000) NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uk_notifications_event_id UNIQUE (event_id),
    -- FK lógica para processed_events: gravadas na mesma transação
    CONSTRAINT fk_notifications_processed_event FOREIGN KEY (event_id) REFERENCES processed_events (event_id),
    CONSTRAINT ck_notifications_recipient_type CHECK (recipient_type IN ('TUTOR', 'CAREGIVER')),
    CONSTRAINT ck_notifications_channel CHECK (channel IN ('LOG', 'EMAIL_SIMULATED'))
);

CREATE INDEX idx_notifications_booking_id ON notifications (booking_id);
