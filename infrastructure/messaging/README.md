# Mensageria (RabbitMQ)

Decisão: [ADR-004](../../docs/adr/ADR-004-mensageria.md) · Contrato: [eventos](../../docs/api/eventos.md).
O RabbitMQ é definido no `docker-compose.yml` (serviço `rabbitmq`). A topologia **não** é criada
por scripts: a API e o `notification-worker` a declaram ao conectar (Spring AMQP `Declarables`).

## Topologia

| Elemento | Nome | Detalhe |
|---|---|---|
| Exchange | `booking.events` | `topic`, durável |
| Fila | `notification.booking-events` | durável, binding `booking.*`, `x-dead-letter-exchange=booking.events.dlx` |
| DLX | `booking.events.dlx` | `fanout`, durável |
| DLQ | `notification.booking-events.dlq` | mensagens inválidas ou que falharam 3 vezes |

```text
API ──▶ booking.events ──booking.*──▶ notification.booking-events ──▶ notification-worker
                                              │ rejeitada (inválida ou 3 falhas)
                                              ▼
                                   booking.events.dlx ──▶ notification.booking-events.dlq
```

## Painel de gestão

Com o Compose no ar, acesse <http://localhost:15672> com o usuário/senha definidos em
`RABBITMQ_USER` / `RABBITMQ_PASSWORD` (valores locais em `.env.example`).

- **Exchanges → `booking.events`**: bindings e taxa de publicação.
- **Queues → `notification.booking-events`**: mensagens prontas (*Ready*), em processamento
  (*Unacked*) e consumidores conectados. Fila crescendo = worker parado ou lento
  (alerta `RabbitMqQueueBacklog`, ADR-005).

Publicar um evento de teste manualmente: **Exchanges → `booking.events` → Publish message**,
routing key `booking.created`, propriedade `content_type=application/json`, delivery mode 2 e o
JSON do exemplo em [eventos.md](../../docs/api/eventos.md).

## Ver a DLQ

**Queues → `notification.booking-events.dlq` → Get messages** (modo *Nack message requeue true*
para só olhar sem remover). O header `x-death` mostra a fila de origem, o motivo (`rejected`) e
quantas vezes a mensagem morreu. O motivo detalhado está no log do worker:
`Event rejected to DLQ reason="…"` ou `Event processing failed eventId=…`.

## Reprocessar

1. Corrija a causa (ex.: banco do worker de volta, bug corrigido e redeploy).
2. Mova as mensagens da DLQ para a exchange principal. Opções:
   - **Shovel pelo painel** (plugins `rabbitmq_shovel` e `rabbitmq_shovel_management` já
     habilitados em `infrastructure/docker/rabbitmq/enabled_plugins`): fila
     `notification.booking-events.dlq` → *Move messages* → destino `notification.booking-events`;
   - **Manual**: *Get messages* na DLQ (modo *Ack message requeue false*), copie o payload e
     publique em `booking.events` com a routing key original (campo `routing_keys` do `x-death`).
3. Reprocessar é seguro: o worker é idempotente por `eventId`, então um evento que já tinha sido
   processado é apenas confirmado e ignorado.

Mensagens malformadas ou com `eventType` desconhecido voltarão para a DLQ: devem ser analisadas e
descartadas (*Purge* da DLQ) ou corrigidas pelo publicador.

## Inspeção por linha de comando

```bash
docker compose exec rabbitmq rabbitmqctl list_queues name messages messages_unacknowledged consumers
docker compose exec rabbitmq rabbitmqctl list_bindings source_name destination_name routing_key
```
