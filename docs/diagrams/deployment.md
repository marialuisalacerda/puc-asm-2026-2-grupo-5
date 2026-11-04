# Deployment — ambiente local (Docker Compose)

Decisão: [ADR-006](../adr/ADR-006-containerizacao.md) · Arquivo: [`docker-compose.yml`](../../docker-compose.yml) · Guia: [infrastructure/docker](../../infrastructure/docker/README.md) · Visão lógica: [C2](../c4/C2-containers.md).

Projeto Compose `plantao-pet`, rede bridge única `backend`. O mobile **não** roda no Compose: o Expo roda no host do desenvolvedor e o app abre em emulador, navegador ou dispositivo físico.

## Diagrama

```mermaid
flowchart TB
    subgraph dev["Dispositivo / emulador / navegador"]
        app["App Plantão de Pet<br/><i>Expo Go ou build de desenvolvimento</i>"]
    end

    subgraph host["Host do desenvolvedor (Windows, macOS ou Linux com Docker)"]
        metro["Expo CLI / Metro bundler<br/><i>npx expo start · fora do Compose</i>"]

        subgraph compose["Docker Engine · projeto plantao-pet · rede backend"]
            direction TB

            subgraph appprofile["profile app (docker compose --profile app up -d)"]
                api["api<br/>plantao-pet/api:local<br/>:8080"]
                worker["notification-worker<br/>plantao-pet/notification-worker:local<br/>:8081 (host 8082)"]
            end

            subgraph dados["Dados e mensageria (sobem por padrão)"]
                pg[("postgres<br/>postgres:16.4-alpine<br/>:5432")]
                ndb[("notification-db<br/>postgres:16.4-alpine<br/>:5432 só na rede interna")]
                mq["rabbitmq<br/>rabbitmq:3.13.7-management-alpine<br/>:5672 · :15672 · :15692 interno"]
            end

            subgraph obs["Observabilidade (sobem por padrão)"]
                otel["otel-collector<br/>contrib 0.111.0<br/>:4317 · :4318 · :8889 interno"]
                prom["prometheus<br/>v2.54.1 · :9090"]
                pexp["postgres-exporter<br/>v0.15.0 · :9187 interno"]
                jaeger["jaeger<br/>all-in-one 1.60.0 · :16686"]
                loki["loki<br/>3.1.1 · :3100 interno"]
                graf["grafana<br/>11.2.0 · :3000"]
            end
        end

        subgraph vols["Volumes nomeados"]
            v1[("postgres-data")]
            v2[("notification-db-data")]
            v3[("rabbitmq-data")]
            v4[("prometheus-data")]
            v5[("loki-data")]
            v6[("grafana-data")]
        end
    end

    app -->|"carrega o bundle JS"| metro
    app -->|"HTTP · EXPO_PUBLIC_API_URL<br/>host:8080"| api
    api -->|"JDBC postgres:5432"| pg
    api -->|"AMQP rabbitmq:5672"| mq
    worker -->|"AMQP rabbitmq:5672"| mq
    worker -->|"JDBC notification-db:5432"| ndb
    api -->|"OTLP otel-collector:4318"| otel
    worker -->|"OTLP otel-collector:4318"| otel
    otel --> jaeger
    otel --> loki
    prom -->|"scrape"| otel
    prom -->|"scrape"| mq
    prom -->|"scrape"| pexp
    pexp --> pg
    graf --> prom
    graf --> jaeger
    graf --> loki

    pg -.- v1
    ndb -.- v2
    mq -.- v3
    prom -.- v4
    loki -.- v5
    graf -.- v6

    classDef appc fill:#1168bd,stroke:#0b4884,color:#ffffff
    classDef dbc fill:#2f7d4f,stroke:#1f5535,color:#ffffff
    classDef obsc fill:#6b7b8c,stroke:#46525e,color:#ffffff
    classDef vol fill:#eeeeee,stroke:#999999,color:#000000
    classDef outside fill:#ffffff,stroke:#1168bd,color:#000000,stroke-dasharray: 5 5
    class api,worker,mq appc
    class pg,ndb dbc
    class otel,prom,pexp,jaeger,loki,graf obsc
    class v1,v2,v3,v4,v5,v6 vol
    class app,metro outside
```

Dentro da rede `backend`, os containers se encontram pelo **nome do serviço** (`postgres`, `rabbitmq`, `otel-collector`...). As portas publicadas abaixo são as que o host (e o celular) enxergam.

## Portas publicadas no host

Todas configuráveis no `.env` (ver `.env.example`).

| Serviço | Porta no host | Variável | Uso |
|---|---|---|---|
| api | 8080 | `API_PORT` | REST para o mobile; `/actuator/health` |
| notification-worker | 8082 (→ 8081 no container) | `WORKER_PORT` | `GET /notifications`; `/actuator/health` |
| postgres | 5432 | `DB_PORT` | acesso local ao banco da API (desenvolvimento) |
| rabbitmq | 5672 · 15672 | `RABBITMQ_PORT` · `RABBITMQ_MANAGEMENT_PORT` | AMQP · painel de gestão |
| otel-collector | 4317 · 4318 | `OTLP_GRPC_PORT` · `OTLP_HTTP_PORT` | OTLP para serviços rodando fora do Compose |
| prometheus | 9090 | `PROMETHEUS_PORT` | consultas e alertas |
| jaeger | 16686 | `JAEGER_PORT` | interface de traces |
| grafana | 3000 | `GRAFANA_PORT` | dashboards |

Somente na rede interna (sem porta no host): `notification-db` (5432), `loki` (3100), `postgres-exporter` (9187), métricas do RabbitMQ (15692) e do Collector (8888/8889).

## Profiles e ordem de subida

| Comando | Sobe |
|---|---|
| `docker compose up -d` | postgres, notification-db, rabbitmq e toda a observabilidade |
| `docker compose --profile app up -d` | o anterior + `api` e `notification-worker` (imagens a partir de `infrastructure/docker/java-service.dockerfile`) |

- `api` espera `postgres` e `rabbitmq` saudáveis (healthcheck) e o `otel-collector` iniciado.
- `notification-worker` espera `notification-db` e `rabbitmq` saudáveis e o `otel-collector` iniciado; não depende de `postgres` (ownership, ADR-003).
- O Flyway de cada serviço migra o próprio banco na inicialização.

## Mobile fora do Compose

```bash
cd apps/mobile
npx expo start
```

- `EXPO_PUBLIC_API_URL` padrão `http://localhost:8080` funciona no navegador e no simulador iOS. No emulador Android use `http://10.0.2.2:8080`; em dispositivo físico, o IP do host na rede local (`http://192.168.x.y:8080`).
- `EXPO_PUBLIC_USE_MOCKS=true` dispensa o backend (implementação Mock da `PlantaoPetApi`).
- O Metro do Expo usa a porta **8081** do host; por isso o `notification-worker` é publicado na **8082** (internamente continua 8081).

## Fora do escopo

Não há ambiente de nuvem nem deploy automatizado (CD); o ADR-007 limita o pipeline a CI. Se um ambiente remoto for definido, este diagrama ganha um segundo nó de implantação e o ADR correspondente.
