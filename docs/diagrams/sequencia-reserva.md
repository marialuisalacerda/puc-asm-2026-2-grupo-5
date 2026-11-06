# Sequência — ciclo da reserva

Fluxo principal do projeto: o tutor solicita uma reserva, o cuidador é notificado, aceita, e o tutor é notificado. Componentes: [C3 da API](../c4/C3-componentes-api.md) e [C3 do worker](../c4/C3-componentes-worker.md). Contratos: [OpenAPI](../api/openapi.yaml), [eventos](../api/eventos.md), [regras de negócio](../architecture/contratos-integracao.md#regras-de-negócio-da-reserva).

## 1. Criação da reserva e notificação do cuidador

```mermaid
sequenceDiagram
    autonumber
    actor Tutor
    participant App as App mobile
    participant API as API (BookingController / BookingService)
    participant Price as PriceCalculator (Strategy)
    participant DBA as PostgreSQL da API
    participant MQ as RabbitMQ (booking.events)
    participant W as notification-worker
    participant DBN as PostgreSQL de notificações
    actor Cuidador

    Tutor->>App: escolhe pet, cuidador, serviço e período
    App->>API: POST /bookings (JSON)
    activate API
    API->>API: valida scheduledEnd > scheduledStart (400) e início no futuro (422)
    API->>DBA: busca pet e cuidador
    DBA-->>API: pet do tutor? cuidador ativo?
    API->>DBA: existe reserva REQUESTED/ACCEPTED sobreposta do cuidador?
    DBA-->>API: não
    API->>Price: calcular(serviceType, período)
    Price-->>API: priceCents (ex. WALK 2h = 6000)
    API->>DBA: INSERT booking status REQUESTED
    Note over API,DBA: commit da transação
    API-->>App: 201 Created (Booking, status REQUESTED)
    deactivate API
    App-->>Tutor: reserva solicitada
    API->>MQ: AFTER_COMMIT publica BookingCreated (routing key booking.created)
    MQ->>W: entrega da fila notification.booking-events
    activate W
    W->>DBN: eventId já está em processed_events?
    DBN-->>W: não
    W->>DBN: INSERT notification (cuidador) + INSERT processed_events (mesma transação)
    W-->>MQ: ack
    deactivate W
    W--)Cuidador: notificação registrada (consultada via GET /notifications ou app)
```

## 2. Aceite pelo cuidador e notificação do tutor

```mermaid
sequenceDiagram
    autonumber
    actor Cuidador
    participant App as App mobile
    participant API as API (BookingService / Booking)
    participant DBA as PostgreSQL da API
    participant MQ as RabbitMQ (booking.events)
    participant W as notification-worker
    participant DBN as PostgreSQL de notificações
    actor Tutor

    Cuidador->>App: abre a reserva recebida e aceita
    App->>API: PATCH /bookings/{id}/accept
    activate API
    API->>DBA: carrega booking
    DBA-->>API: status REQUESTED
    API->>API: booking.accept() (REQUESTED para ACCEPTED)
    API->>DBA: UPDATE status ACCEPTED
    Note over API,DBA: commit da transação
    API-->>App: 200 OK (status ACCEPTED)
    deactivate API
    API->>MQ: AFTER_COMMIT publica BookingAccepted (booking.accepted)
    MQ->>W: entrega
    activate W
    W->>DBN: verifica eventId, grava notificação do tutor e eventId
    W-->>MQ: ack
    deactivate W
    W--)Tutor: notificação registrada "reserva aceita"
```

Rejeição (`PATCH /bookings/{id}/reject`, evento `BookingRejected`) e cancelamento (`PATCH /bookings/{id}/cancel`, evento `BookingCancelled`) seguem a mesma sequência, com `reason` opcional no evento.

## 3. Caminhos alternativos

### 3.1 Conflito de agenda (409) e transição inválida (409)

```mermaid
sequenceDiagram
    autonumber
    participant App as App mobile
    participant API as API (BookingService)
    participant EH as Handler de erros (RFC 7807)
    participant DBA as PostgreSQL da API

    App->>API: POST /bookings
    API->>DBA: existe reserva REQUESTED/ACCEPTED sobreposta do cuidador?
    DBA-->>API: sim
    API->>EH: lança exceção de conflito
    EH-->>App: 409 application/problem+json (sobreposição de horário)
    Note over API,DBA: nada é gravado e nenhum evento é publicado

    App->>API: PATCH /bookings/{id}/accept (reserva já REJECTED)
    API->>DBA: carrega booking
    DBA-->>API: status REJECTED
    API->>EH: booking.accept() recusa a transição
    EH-->>App: 409 application/problem+json (transição inválida)
```

Outras respostas de erro da criação: 400 (período invertido), 404 (cuidador inexistente), 422 (início no passado, pet de outro tutor, cuidador inativo).

### 3.2 Mensagem duplicada (Idempotent Consumer)

```mermaid
sequenceDiagram
    autonumber
    participant MQ as RabbitMQ
    participant W as notification-worker
    participant DBN as PostgreSQL de notificações

    Note over MQ,W: o mesmo BookingCreated (mesmo eventId) é entregue de novo, por exemplo após queda do worker antes do ack
    MQ->>W: entrega eventId = 4f0c...
    W->>DBN: eventId já está em processed_events?
    DBN-->>W: sim
    W->>W: métrica message.processing outcome=duplicate
    W-->>MQ: ack (descarta sem nova notificação)
```

### 3.3 Falha no processamento: retry e DLQ

```mermaid
sequenceDiagram
    autonumber
    participant MQ as RabbitMQ (notification.booking-events)
    participant W as notification-worker
    participant DBN as PostgreSQL de notificações
    participant DLX as booking.events.dlx
    participant DLQ as notification.booking-events.dlq

    MQ->>W: entrega BookingCreated
    W->>DBN: grava notificação
    DBN--xW: erro (banco indisponível)
    Note over W: tentativa 1 falhou, espera 1s
    W->>DBN: tentativa 2
    DBN--xW: erro
    Note over W: espera 2s
    W->>DBN: tentativa 3
    DBN--xW: erro
    W->>W: métrica message.processing.errors
    W-->>MQ: reject sem requeue
    MQ->>DLX: dead-letter
    DLX->>DLQ: mensagem preservada para análise
    Note over DLQ: reprocessamento manual pelo painel do RabbitMQ

    MQ->>W: entrega mensagem com JSON inválido ou eventType desconhecido
    W-->>MQ: reject sem requeue, sem retentativa
    MQ->>DLX: dead-letter
    DLX->>DLQ: mensagem inválida isolada
```

## Rastreabilidade

O OpenTelemetry Java Agent propaga o contexto de trace pelos headers AMQP, então os passos da API e do worker aparecem em **um único trace** no Jaeger (`http://localhost:16686`), e os logs carregam o mesmo `trace_id` no Loki ([ADR-005](../adr/ADR-005-observabilidade.md)).
