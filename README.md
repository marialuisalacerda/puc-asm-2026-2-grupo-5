# Trabalho Final · Arquitetura de Software Multiplataforma
PUC MINAS - Pós-Graduação em Arquitetura de Software Distribuído

Grupo 05 · ASM 2026/2

## Integrantes

| Integrante | Responsabilidade principal | Matrícula |
|---|---|---|
| Integrante 1 | Arquitetura e C4 | — |
| Integrante 2 | Aplicativo Mobile | — |
| Integrante 3 | Backend e domínio | — |
| Maria Luísa Lacerda | Banco de dados | 257115 |
| Integrante 5 | Mensageria | — |
| Integrante 6 | Docker e Observabilidade | — |
| Integrante 7 | CI/CD e Qualidade | — |

---

## Domínio

O sistema tem como objetivo conectar tutores de pets a cuidadores que possam auxiliar em diferentes tipos de serviços, como:

- passeios;
- companhia;
- visitas domiciliares;
- hospedagem;
- cuidados temporários.

O fluxo principal começa quando um tutor cadastra seu pet, pesquisa cuidadores disponíveis e solicita um serviço.

O cuidador pode aceitar ou rejeitar a solicitação. As alterações relevantes no ciclo de vida da reserva podem gerar eventos assíncronos utilizados, por exemplo, para processamento de notificações.

O projeto busca demonstrar conceitos de arquitetura multiplataforma, comunicação entre serviços, persistência, mensageria, observabilidade, CI/CD e desenvolvimento distribuído.

As decisões arquiteturais relevantes devem ser registradas em:

```text
docs/adr/
```

---

## Objetivos arquiteturais

O projeto deve contemplar:

- modelagem C4;
- serviço/API;
- aplicação multiplataforma;
- banco de dados;
- estratégia de dados distribuídos ou ownership dos dados;
- mensageria;
- observabilidade;
- Design Patterns;
- Docker;
- CI/CD;
- documentação de decisões por ADR;
- diagramas arquiteturais.

---

## Arquitetura

A arquitetura inicial proposta é:

```text
Tutor / Cuidador
       |
       v
Aplicativo Mobile
       |
       | HTTP / REST
       v
API Java / Spring Boot
    |             |
    |             |
    v             v
PostgreSQL     RabbitMQ
                  |
                  v
          Notification Worker

API / Worker
     |
     v
Observabilidade
```

A arquitetura pode evoluir durante o projeto, desde que alterações relevantes sejam registradas por ADR e refletidas no C4.

---

## Stack

### Mobile

- React Native;
- Expo;
- TypeScript.

### Backend

- Java;
- Spring Boot;
- Spring Web;
- Spring Data JPA;
- Maven.

### Banco de dados

- PostgreSQL;
- Flyway para migrations.

### Mensageria

- RabbitMQ;
- Spring AMQP.

### Observabilidade

- OpenTelemetry;
- Prometheus;
- Grafana.

### Infraestrutura

- Docker;
- Docker Compose.

### CI/CD

- GitHub Actions.

---

## Estrutura prevista

```text
/
├── apps/
│   └── mobile/
│
├── services/
│   ├── api/
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/
│   │       │   └── resources/
│   │       │       ├── application.yml
│   │       │       └── db/
│   │       │           └── migration/
│   │       └── test/
│   └── notification-worker/
│
├── infrastructure/
│   ├── database/
│   │   └── README.md
│   ├── messaging/
│   ├── observability/
│   └── docker/
│
├── docs/
│   ├── adr/
│   ├── architecture/
│   ├── c4/
│   ├── diagrams/
│   ├── database/
│   │   └── README.md
│   └── api/
│
├── .github/
│   └── workflows/
│
├── docker-compose.yml
├── README.md
├── AGENTS.md
└── .gitignore
```

> **Banco de dados:** as migrations Flyway que definem e evoluem o schema ficam em
> `services/api/src/main/resources/db/migration/`. A pasta `infrastructure/database/`
> concentra apenas artefatos operacionais/de infraestrutura do PostgreSQL e não deve
> duplicar migrations. A documentação de modelagem, convenções e ownership fica em
> `docs/database/`, enquanto o DER fica em `docs/diagrams/`.

---

## Aplicativo Mobile

O aplicativo será responsável pela interação com tutores e cuidadores.

Entre os principais fluxos previstos estão:

- cadastro e consulta de pets;
- consulta de cuidadores;
- visualização de serviços disponíveis;
- solicitação de serviço;
- acompanhamento das reservas;
- aceite ou rejeição pelo cuidador;
- consulta do status de uma reserva.

Enquanto a API ainda não estiver disponível, o aplicativo poderá utilizar mocks e dados simulados.

---

## API

A API principal será implementada em Java com Spring Boot.

Ela será responsável por:

- expor endpoints REST;
- aplicar regras de negócio;
- gerenciar pets;
- gerenciar cuidadores;
- gerenciar reservas;
- persistir dados;
- publicar eventos;
- integrar-se ao banco de dados;
- integrar-se ao broker de mensagens.

Entidades iniciais previstas:

```text
Tutor
Pet
Caregiver
Booking
```

Endpoints iniciais sugeridos:

```text
POST /pets
GET /pets/{id}

GET /caregivers
GET /caregivers/{id}

POST /bookings
GET /bookings
GET /bookings/{id}

PATCH /bookings/{id}/accept
PATCH /bookings/{id}/reject
PATCH /bookings/{id}/cancel
```

Os contratos definitivos deverão ser documentados antes da integração entre mobile e backend.

---

## Design Patterns

O projeto deverá demonstrar padrões aplicados a necessidades reais da solução.

### Repository

Responsável por desacoplar as regras de negócio da persistência.

```text
BookingService
      |
      v
BookingRepository
      |
      v
PostgreSQL
```

### Strategy

Pode ser utilizado para regras diferentes de precificação ou comportamento por tipo de serviço.

```text
PriceStrategy
    |
    +-- WalkPriceStrategy
    +-- HostingPriceStrategy
    +-- CompanionPriceStrategy
```

### Dependency Injection

As dependências devem ser fornecidas pelo Spring, preferencialmente por construtor.

### Publish/Subscribe

A comunicação assíncrona entre a API e seus consumidores será realizada através de eventos publicados no RabbitMQ.

---

## Banco de dados

O PostgreSQL será utilizado como banco relacional principal e o Flyway será utilizado para versionar a evolução do schema.

O modelo inicial deverá representar pelo menos:

```text
Tutor
  |
  +---- Pet

Tutor
  |
  +---- Booking
          |
          +---- Caregiver
          |
          +---- Pet
```

A estratégia de ownership dos dados deverá ser documentada para evitar que componentes independentes acessem diretamente dados pelos quais não são responsáveis.

Os artefatos de banco deverão seguir estes caminhos:

```text
services/api/src/main/resources/db/migration/   # migrations Flyway: schema e seeds versionados
infrastructure/database/                        # artefatos operacionais/de infraestrutura do PostgreSQL
docs/database/                                  # documentação de modelagem, convenções e ownership
docs/diagrams/                                  # DER e demais diagramas
docs/adr/ADR-003-persistencia.md                # decisão arquitetural de persistência
```

As alterações estruturais e os dados iniciais controlados pelo projeto devem ser versionados por migrations. Não devem existir scripts concorrentes de criação do schema fora do diretório de migrations do Flyway.

---

## Mensageria

O RabbitMQ será utilizado para comunicação assíncrona.

Eventos iniciais sugeridos:

| Evento | Responsabilidade |
|---|---|
| `BookingCreated` | informa que uma solicitação foi criada |
| `BookingAccepted` | informa que o cuidador aceitou a solicitação |
| `BookingRejected` | informa que a solicitação foi rejeitada |
| `BookingCancelled` | informa que a solicitação foi cancelada |

Fluxo principal:

```text
API
 |
 | BookingCreated
 v
RabbitMQ
 |
 v
Notification Worker
 |
 v
Processamento da notificação
```

Cada evento deverá possuir uma identidade própria.

Estrutura mínima recomendada:

```json
{
  "eventId": "uuid",
  "eventType": "BookingCreated",
  "version": 1,
  "occurredAt": "2026-10-14T20:00:00Z",
  "data": {}
}
```

Consumidores deverão considerar mensagens duplicadas e aplicar idempotência quando necessário.

---

## Observabilidade

A solução deverá demonstrar os três pilares de observabilidade:

```text
Logs
Metrics
Traces
```

Tecnologias inicialmente previstas:

```text
OpenTelemetry
Prometheus
Grafana
```

Métricas possíveis:

```text
http_requests_total
booking_created_total
booking_rejected_total
message_processing_total
message_processing_errors_total
```

O dashboard deverá permitir observar, sempre que possível:

- volume de requisições;
- reservas criadas;
- taxa de erro;
- tempo de resposta;
- mensagens processadas;
- falhas no processamento.

---

## Docker

Os componentes do backend deverão ser executáveis em containers.

O ambiente local deverá ser centralizado no Docker Compose.

Estrutura esperada:

```text
Docker Compose
 |
 +-- API
 |
 +-- PostgreSQL
 |
 +-- RabbitMQ
 |
 +-- Notification Worker
 |
 +-- Prometheus
 |
 +-- Grafana
```

Objetivo de execução:

```bash
docker compose up -d
```

Sempre que possível, este comando deverá subir toda a infraestrutura necessária ao backend.

---

## Modelagem C4

O C4 deverá ser iniciado antes da implementação e atualizado conforme a arquitetura evoluir.

Serão priorizados:

### Nível 1 — System Context

Representa:

- tutor;
- cuidador;
- sistema;
- sistemas externos relevantes.

### Nível 2 — Container

Representa:

- aplicativo mobile;
- API;
- banco;
- broker;
- worker;
- componentes de observabilidade.

### Nível 3 — Component

Será utilizado principalmente para detalhar a organização interna da API.

Exemplo:

```text
Controller
    |
    v
Service
    |
    +---- Repository
    |
    +---- Strategy
    |
    +---- Event Publisher
```

O nível 4 somente será produzido se necessário.

Os modelos deverão ficar em:

```text
docs/c4/
```

---

## ADRs

Cada responsável técnico deverá registrar as decisões arquiteturais relevantes de sua área.

Fluxo esperado:

```text
Problema
   |
   v
Alternativas
   |
   v
Decisão
   |
   v
ADR
   |
   v
Implementação
```

Exemplos de ADRs previstos:

```text
ADR-001-stack-mobile.md
ADR-002-arquitetura-backend.md
ADR-003-persistencia.md
ADR-004-mensageria.md
ADR-005-observabilidade.md
ADR-006-containerizacao.md
ADR-007-ci-cd.md
```

Os ADRs deverão ficar em:

```text
docs/adr/
```

O responsável técnico propõe e documenta a decisão. O responsável pela arquitetura verifica sua consistência com a arquitetura global e com o C4.

---

## Diagramas

Além do C4, o projeto deverá possuir, quando aplicável:

- DER;
- diagrama de sequência do fluxo principal;
- diagrama de mensageria;
- diagrama de deployment.

Os diagramas deverão ficar em:

```text
docs/diagrams/
```

---

## Estratégia Git

O repositório utilizará:

```text
main
develop
```

### `develop`

A branch `develop` será a branch principal de integração durante o desenvolvimento.

Nenhum integrante deverá desenvolver diretamente nela.

Fluxo padrão:

```text
branch de trabalho
        |
        v
   Pull Request
        |
        v
     develop
```

Todas as funcionalidades, correções, documentações e alterações de infraestrutura deverão ser integradas primeiro em `develop`.

### `main`

A `main` representa a versão estável destinada à entrega.

Durante o desenvolvimento normal, nenhuma feature deverá ser enviada diretamente para `main`.

O fluxo final será:

```text
branches de trabalho
        |
        v
      develop
        |
        v
  validação final
        |
        v
   Pull Request
        |
        v
       main
```

O merge de `develop` para `main` deverá ocorrer apenas quando:

- funcionalidades estiverem concluídas;
- integrações estiverem funcionando;
- testes estiverem passando;
- CI estiver verde;
- Docker estiver funcional;
- C4 estiver atualizado;
- ADRs estiverem atualizados;
- documentação estiver revisada.

---

## Branches

Toda branch de trabalho deve nascer da versão mais recente de `develop`.

Exemplo:

```bash
git checkout develop
git pull origin develop
git checkout -b feat/create-booking
```

Convenção:

```text
feat/<descricao>
fix/<descricao>
infra/<descricao>
docs/<descricao>
test/<descricao>
refactor/<descricao>
```

Exemplos:

```text
feat/create-booking
feat/caregiver-search
infra/rabbitmq
infra/observability
docs/c4-container
fix/booking-validation
```

---

## Commits semânticos

Os commits devem seguir preferencialmente:

```text
tipo(escopo): descrição curta
```

| Tipo | Quando utilizar |
|---|---|
| `feat` | nova funcionalidade |
| `fix` | correção |
| `docs` | documentação, C4 ou ADR |
| `infra` | Docker, observabilidade ou infraestrutura |
| `test` | testes |
| `refactor` | alteração estrutural sem mudar comportamento |
| `chore` | configuração ou manutenção do projeto |

Exemplos:

```text
feat(api): implementa criação de reserva
feat(messaging): publica BookingCreated
docs(c4): adiciona diagrama de containers
docs(adr): registra escolha do RabbitMQ
infra(observability): configura Prometheus
test(api): adiciona testes do BookingService
```

---

## Pull Requests

Toda alteração relevante deverá passar por Pull Request.

Durante o desenvolvimento:

```text
branch de trabalho → develop
```

Na entrega final:

```text
develop → main
```

Cada Pull Request deverá informar:

- objetivo da alteração;
- contexto ou motivação da alteração, quando necessário;
- como testar;
- evidências quando necessário;
- possíveis impactos arquiteturais.

---

## Branch Protection

As branches:

```text
develop
main
```

devem possuir proteção.

Regras recomendadas:

- impedir push direto;
- exigir Pull Request;
- exigir pelo menos uma aprovação;
- exigir pipeline verde;
- impedir merge com testes quebrados.

A `main` deve possuir proteção mais rígida por representar a versão estável da solução.

---

## CI/CD

O GitHub Actions deverá validar automaticamente as principais partes do projeto.

Pipelines inicialmente previstas:

```text
backend-ci
mobile-ci
docker-ci
```

Backend:

```text
checkout
   |
   v
maven build
   |
   v
test
```

Mobile:

```text
checkout
   |
   v
npm ci
   |
   v
lint
   |
   v
test
```

Docker:

```text
docker build
     |
     v
docker compose config
```

Pull Requests com pipelines obrigatórias não deverão ser integradas enquanto houver falhas.

---

## Desenvolvimento distribuído

O projeto deverá priorizar contratos antes da implementação.

Princípio:

> Depender de contratos, e não da implementação de outro integrante.

Exemplos:

- mobile pode usar mocks enquanto a API não estiver disponível;
- backend pode começar com dependências simuladas;
- mensageria pode utilizar eventos de teste;
- banco pode ser modelado com base nos contratos;
- observabilidade pode começar com uma aplicação mínima;
- CI/CD pode ser criada antes da implementação completa.

Isso permite que os integrantes trabalhem de forma paralela.

---

## Divisão de responsabilidades

### Integrante 1 — Arquitetura e C4

Responsável por:

- C4 nível 1;
- C4 nível 2;
- C4 nível 3;
- visão arquitetural;
- atualização dos modelos;
- consistência entre ADRs e arquitetura.

### Integrante 2 — Mobile

Responsável por:

- React Native/Expo;
- telas;
- navegação;
- gerenciamento de estado;
- mocks;
- integração com API.

### Integrante 3 — Backend

Responsável por:

- Java/Spring Boot;
- API REST;
- domínio;
- regras de negócio;
- Repository;
- Strategy;
- Dependency Injection;
- testes.

### Integrante 4 — Banco de Dados

Responsável por:

- PostgreSQL;
- DER;
- migrations;
- seeds;
- índices;
- constraints;
- ownership dos dados.

### Integrante 5 — Mensageria

Responsável por:

- RabbitMQ;
- contratos dos eventos;
- publisher;
- consumer;
- worker;
- retry;
- idempotência.

### Integrante 6 — Docker e Observabilidade

Responsável por:

- Dockerfiles;
- Docker Compose;
- OpenTelemetry;
- logs;
- métricas;
- traces;
- Prometheus;
- Grafana.

### Integrante 7 — CI/CD e Qualidade

Responsável por:

- GitHub Actions;
- build;
- testes;
- lint;
- Pull Requests;
- branch protection;
- critérios de qualidade.

---

## Documentação

A documentação do projeto deverá permanecer no próprio repositório.

Estrutura prevista:

```text
docs/
├── adr/
├── architecture/
├── c4/
├── diagrams/
├── database/
└── api/
```

Principais referências:

- [Padrões obrigatórios do repositório](AGENTS.md)
- `docs/adr/` — decisões arquiteturais
- `docs/c4/` — modelagem C4
- `docs/diagrams/` — diagramas complementares, incluindo o DER
- `docs/database/` — documentação de modelagem, convenções e ownership dos dados
- `docs/api/` — contratos da API
- `docs/architecture/` — documentação arquitetural geral

---

## Uso de Inteligência Artificial

Ferramentas de IA podem ser utilizadas para auxiliar na implementação.

Antes de implementar qualquer tarefa, o agente deve consultar:

1. `AGENTS.md`;
2. ADR relacionado;
3. contratos existentes;
4. documentação da área;
5. código já implementado.

A IA não deve redefinir a arquitetura sem justificar e registrar a mudança.

Durante o desenvolvimento, a IA deve considerar `develop` como branch de integração.

Nunca orientar implementação diretamente em `main`.

---

## Definition of Done

Uma tarefa somente será considerada concluída quando:

- código compilar;
- funcionalidade estiver operacional;
- testes relevantes passarem;
- contratos forem respeitados;
- nenhuma credencial estiver versionada;
- documentação relacionada estiver atualizada;
- alterações arquiteturais estiverem refletidas em C4 ou ADR quando necessário;
- pipeline estiver verde;
- Pull Request tiver sido revisada.

---

## Fluxo principal do projeto

```text
Tutor
 |
 v
Aplicativo Mobile
 |
 | solicita serviço
 v
API
 |
 +------> PostgreSQL
 |
 | BookingCreated
 v
RabbitMQ
 |
 v
Notification Worker
 |
 v
Processamento da notificação
```

Esse fluxo será utilizado como referência para integração, mensageria, observabilidade, testes e apresentação final do projeto.