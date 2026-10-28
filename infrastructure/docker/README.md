# Docker

Decisão registrada no [ADR-006](../../docs/adr/ADR-006-containerizacao.md).

## Subir o ambiente

```bash
cp .env.example .env                  # ajuste portas/credenciais locais se quiser
docker compose up -d                  # PostgreSQL, RabbitMQ e observabilidade
docker compose --profile app up -d    # + API e notification-worker
./scripts/smoke-test.sh               # fluxo de ponta a ponta
docker compose ps
docker compose down                   # -v para apagar volumes
```

## Serviços e portas

| Serviço | URL / porta local | Observação |
|---|---|---|
| API | http://localhost:8080 | profile `app` |
| notification-worker | http://localhost:8082 | profile `app`; `GET /notifications` |
| PostgreSQL (API) | localhost:5432 | credenciais em `.env` |
| PostgreSQL (worker) | — (só na rede interna) | `notification-db`, banco próprio do worker (ADR-003) |
| RabbitMQ | localhost:5672 · http://localhost:15672 | painel de gestão |
| Grafana | http://localhost:3000 | `admin`/`admin` (local) |
| Prometheus | http://localhost:9090 | alvos em `/targets` |
| Jaeger | http://localhost:16686 | traces |
| OTel Collector | localhost:4317 (gRPC) · 4318 (HTTP) | OTLP |

Todas as portas publicadas são configuráveis no `.env` (útil se outro projeto já usa 8080/9090).

## Imagem dos serviços Java

`java-service.dockerfile` é compartilhado pela API e pelo worker. O `docker-compose.yml`
usa a pasta de cada serviço como contexto:

```text
services/<servico>/
├── pom.xml
└── src/
```

Contrato com o backend (integrantes 3 e 5):

- projeto Maven com `spring-boot-maven-plugin` gerando um jar executável em `target/`;
- Java 21 (mesma versão do CI);
- configuração lida de variáveis de ambiente, por exemplo:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:plantao_pet}
    username: ${DB_USERNAME:plantao_pet}
    password: ${DB_PASSWORD:}
  rabbitmq:
    host: ${SPRING_RABBITMQ_HOST:localhost}
```

Build manual de uma imagem:

```bash
docker build -f infrastructure/docker/java-service.dockerfile -t plantao-pet/api:local services/api
```

## RabbitMQ

`rabbitmq/enabled_plugins` habilita `rabbitmq_management`, `rabbitmq_prometheus` e o shovel (reprocessar a DLQ pelo painel)
(métricas em `:15692`). Filas, exchanges e bindings são responsabilidade da mensageria
(`infrastructure/messaging/`).
