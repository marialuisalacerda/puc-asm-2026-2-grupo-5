# Contrato de eventos

Eventos de domínio publicados pela API no RabbitMQ e consumidos pelo `notification-worker`.
Decisão de mensageria: [ADR-004](../adr/ADR-004-mensageria.md). Schema JSON: [booking-event.schema.json](events/booking-event.schema.json).

## Topologia

| Elemento | Nome | Tipo / observação |
|---|---|---|
| Exchange | `booking.events` | `topic`, durável — declarada pela API e pelo worker |
| Routing keys | `booking.created`, `booking.accepted`, `booking.rejected`, `booking.cancelled` | uma por evento |
| Fila | `notification.booking-events` | durável, binding `booking.*`, `x-dead-letter-exchange=booking.events.dlx` |
| Dead-letter exchange | `booking.events.dlx` | `fanout`, durável |
| Dead-letter queue | `notification.booking-events.dlq` | recebe mensagens que falharam após as retentativas |

```text
API ──publish──▶ booking.events (topic) ──booking.*──▶ notification.booking-events ──▶ notification-worker
                                                              │ falhou 3x
                                                              ▼
                                                   booking.events.dlx ──▶ notification.booking-events.dlq
```

## Envelope

Todo evento usa o mesmo envelope (`content-type: application/json`, mensagem persistente):

```json
{
  "eventId": "4f0c1d2e-6a1b-4c39-9d7e-1b2f3a4c5d6e",
  "eventType": "BookingCreated",
  "version": 1,
  "occurredAt": "2026-10-28T20:00:00Z",
  "data": {
    "bookingId": "…uuid…",
    "tutorId": "…uuid…",
    "petId": "…uuid…",
    "caregiverId": "…uuid…",
    "serviceType": "WALK",
    "status": "REQUESTED",
    "scheduledStart": "2026-11-02T13:00:00Z",
    "scheduledEnd": "2026-11-02T15:00:00Z",
    "priceCents": 6000,
    "reason": null
  }
}
```

| `eventType` | Routing key | Quando | `data.status` |
|---|---|---|---|
| `BookingCreated` | `booking.created` | `POST /bookings` | `REQUESTED` |
| `BookingAccepted` | `booking.accepted` | `PATCH /bookings/{id}/accept` | `ACCEPTED` |
| `BookingRejected` | `booking.rejected` | `PATCH /bookings/{id}/reject` | `REJECTED` |
| `BookingCancelled` | `booking.cancelled` | `PATCH /bookings/{id}/cancel` | `CANCELLED` |

Regras:

- `eventId` é um UUID novo por evento e é a **chave de idempotência** do consumidor.
- O evento é publicado **somente após o commit** da transação da reserva.
- O evento carrega os dados de que o consumidor precisa: o worker **não** consulta o banco da API (ADR-003).
- `version` muda apenas em alterações incompatíveis do `data`; campos novos opcionais mantêm a versão.
- `reason` só é preenchido em `BookingRejected` e `BookingCancelled`, quando informado.

## Consumo

- Idempotência: o worker grava `eventId` em `processed_events` (banco próprio) na mesma transação da notificação. Evento repetido é confirmado (ack) e ignorado.
- Retentativa: até **3 tentativas** com backoff exponencial (1s, 2s); depois a mensagem é rejeitada sem requeue e vai para a DLQ.
- Mensagem inválida (JSON malformado ou `eventType` desconhecido): vai direto para a DLQ, sem retentativa.
- Rastreabilidade: o OpenTelemetry propaga o contexto de trace pelos headers AMQP, então API e worker aparecem no mesmo trace.
