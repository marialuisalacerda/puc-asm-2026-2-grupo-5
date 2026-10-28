# ADR-004: Mensageria entre API e notification-worker

Status: Proposto
Data: 2026-10-28
Responsável: [a definir] (Mensageria)
Revisores: [responsável pelo C4] + responsáveis por Backend, Dados e Docker/Observabilidade

## Contexto
Toda criação ou mudança de estado de uma reserva (criada, aceita, recusada, cancelada) precisa
gerar uma notificação para o tutor ou para o cuidador. O envio de notificações é lento e depende
de fornecedores externos (e-mail, push), então não pode bloquear nem derrubar a requisição do
usuário. A arquitetura é um monólito modular com workers assíncronos (ADR-001) e cada componente
é dono do próprio banco: o `notification-worker` **não** lê o banco da API (ADR-003).

Requisitos e restrições que influenciam a decisão:

- desacoplar a API do envio: a reserva é confirmada mesmo com o worker parado;
- não perder eventos se o worker estiver fora do ar (mensagens duráveis, entrega posterior);
- falhas transitórias (banco do worker, fornecedor) devem ser retentadas; falhas permanentes
  não podem travar a fila;
- a entrega do broker é no mínimo uma vez (*at-least-once*): o consumidor precisa ser idempotente;
- o evento deve levar tudo o que o consumidor precisa (envelope `eventId`, `eventType`,
  `version`, `occurredAt`, `data`);
- stack Java/Spring Boot, execução local com Docker Compose (ADR-006) e rastreabilidade de
  ponta a ponta com OpenTelemetry (ADR-005);
- time pequeno: operação simples e sem infraestrutura de cluster.

## Opções consideradas
1. **RabbitMQ (AMQP) com Spring AMQP**
   - prós: roteamento flexível por exchange/routing key; filas duráveis, ack/nack, dead-letter
     exchange nativos; Spring AMQP oferece retry, recoverer e declaração de topologia; painel de
     gestão para inspecionar e reprocessar; imagem leve; suportado pelo agente OTel (propaga o trace
     pelos headers); padrão de mercado para filas de trabalho;
   - contras: mais um componente para operar; sem replay de histórico depois do ack; ordem só é
     garantida por fila e com um consumidor.
2. **Apache Kafka**
   - prós: log durável com replay, alto throughput, particionamento, ecossistema de streaming;
   - contras: superdimensionado para alguns eventos por reserva; mais pesado localmente (broker +
     KRaft/ZooKeeper); retry e DLQ não são nativos (precisam de tópicos extras e código); curva de
     aprendizado maior para o time.
3. **Chamada HTTP síncrona API → worker**
   - prós: simples, sem broker, erro imediato para quem chama;
   - contras: acoplamento temporal (worker fora = reserva falha ou notificação perdida); a API
     precisaria implementar retry, timeout e circuit breaker; aumenta a latência da requisição do
     usuário; não atende o requisito de desacoplamento.
4. **Polling no banco (worker lê tabela de pendências)**
   - prós: sem broker; consistência transacional com a reserva;
   - contras: o worker passaria a ler o banco da API, violando o ownership (ADR-003); latência
     proporcional ao intervalo de polling; carga constante no banco; reimplementa um broker.

## Decisão
Escolhemos a **opção 1 (RabbitMQ com Spring AMQP)** porque atende desacoplamento, durabilidade,
retry e DLQ com recursos nativos, preserva o ownership dos dados e tem custo operacional
compatível com o time. Contrato detalhado em [docs/api/eventos.md](../api/eventos.md).

- **Topologia:** exchange `booking.events` (`topic`, durável), routing keys `booking.created`,
  `booking.accepted`, `booking.rejected`, `booking.cancelled`; fila durável
  `notification.booking-events` com binding `booking.*` e `x-dead-letter-exchange=booking.events.dlx`;
  DLX `booking.events.dlx` (`fanout`) ligada à DLQ `notification.booking-events.dlq`. A API e o
  worker declaram a exchange (declaração idempotente); a fila e a DLQ são do worker.
- **Envelope versionado:** `eventId` (UUID), `eventType`, `version` (1), `occurredAt`, `data`, em
  JSON, mensagem persistente. `version` muda só em alterações incompatíveis.
- **Publicação após o commit:** a API publica o evento somente depois do commit da transação da
  reserva, para nunca anunciar uma reserva que foi desfeita.
- **Retry:** no consumidor, 3 tentativas com backoff exponencial (1s, 2s) via `RetryInterceptor`
  *stateless* do Spring AMQP; esgotadas, `RejectAndDontRequeueRecoverer` rejeita sem requeue e o
  broker envia para a DLQ. Mensagens inválidas (JSON malformado, envelope fora do contrato,
  `eventType` ou `version` desconhecidos) vão direto para a DLQ, sem retentativa.
- **Idempotência:** o worker grava o `eventId` em `processed_events` (banco próprio) na mesma
  transação da notificação. Evento repetido recebe ack e é ignorado (métrica
  `message.processing{outcome=duplicate}`).

## Consequências
Positivas:
- a API responde sem esperar o envio; o worker pode ficar parado e processar depois;
- falhas transitórias se recuperam sozinhas; falhas permanentes ficam isoladas na DLQ, visíveis
  e reprocessáveis pelo painel do RabbitMQ, sem bloquear a fila;
- entregas duplicadas não geram notificação duplicada;
- novos consumidores (ex.: auditoria, analytics) entram com uma nova fila ligada à mesma
  exchange, sem mudar a API;
- trace único API → RabbitMQ → worker pelo agente OpenTelemetry (ADR-005).

Negativas / riscos:
- **perda entre commit e publish:** se a API cair (ou o broker estiver indisponível) depois do
  commit e antes da publicação, o evento se perde e a notificação não sai. Aceito nesta fase
  acadêmica; a evolução recomendada é o **Transactional Outbox** (o evento é gravado numa tabela
  `outbox` na mesma transação da reserva e um relay publica e marca como enviado), que garante
  *at-least-once* também na publicação;
- consistência eventual: a notificação aparece alguns instantes depois da mudança da reserva;
- ordem entre eventos da mesma reserva não é garantida com retry ou com mais de um consumidor
  (o texto de cada notificação é autocontido para tolerar isso);
- a retentativa em memória segura a mensagem no consumidor durante o backoff (até ~3s por
  mensagem); para backoffs longos, evoluir para filas de espera com TTL;
- mais um componente para operar, monitorar (fila acumulada, DLQ) e manter compatível
  (contrato de eventos versionado).

## Impacto no C4
Nível afetado: C2 e C3
O que muda:
- C2: container **RabbitMQ** ("Message broker"); setas API → RabbitMQ ("publica eventos de
  reserva, AMQP"), RabbitMQ → notification-worker ("entrega eventos, AMQP"); container
  **notification-db** (PostgreSQL do worker) com seta notification-worker → notification-db
  ("grava idempotência e notificações, JDBC"). Nenhuma seta do worker para o banco da API;
- C3 (worker): componentes `BookingEventListener` (adaptador AMQP), `BookingEventParser`,
  `NotificationProcessingService` (idempotência), `BookingEventHandler` (Strategy por
  `eventType`), `NotificationSender` (porta de saída), repositórios JPA e `NotificationController`
  (consulta read-only).

## Referências
- [Contrato de eventos](../api/eventos.md) e [schema JSON](../api/events/booking-event.schema.json)
- [Contratos de integração](../architecture/contratos-integracao.md)
- [notification-worker](../../services/notification-worker/README.md) e
  [guia de mensageria](../../infrastructure/messaging/README.md)
- ADR-001 (estilo arquitetural), ADR-003 (persistência e ownership), ADR-005 (observabilidade),
  ADR-006 (containerização)
- RabbitMQ — Dead Letter Exchanges: <https://www.rabbitmq.com/docs/dlx>
- RabbitMQ — Consumer Acknowledgements and Publisher Confirms: <https://www.rabbitmq.com/docs/confirms>
- Spring AMQP — Resilience: Recovering from Errors and Broker Failures:
  <https://docs.spring.io/spring-amqp/reference/amqp/resilience-recovering-from-errors-and-broker-failures.html>
- RICHARDSON, Chris. Pattern: Transactional outbox. <https://microservices.io/patterns/data/transactional-outbox.html>
- RICHARDSON, Chris. Pattern: Idempotent consumer. <https://microservices.io/patterns/communication-style/idempotent-consumer.html>
- HOHPE, Gregor; WOOLF, Bobby. Enterprise Integration Patterns. Addison-Wesley, 2003.
