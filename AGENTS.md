# Contexto do Projeto

Projeto acadêmico multiplataforma desenvolvido por uma equipe de 7 integrantes.

O sistema conecta tutores de pets a cuidadores que oferecem serviços como passeio, companhia, visitas e hospedagem.

## Objetivos arquiteturais

O projeto deve demonstrar:

- C4;
- CI/CD;
- API/serviço;
- mensageria;
- banco de dados distribuído ou separação clara de ownership dos dados;
- observabilidade;
- Design Patterns;
- ADRs;
- Docker;
- diagramas arquiteturais.

## Arquitetura inicial

```text
Mobile
   |
   v
API
   |
   +---- PostgreSQL
   |
   +---- RabbitMQ
             |
             v
           Worker

API / Worker
     |
     v
Observabilidade
```

## Stack inicial

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
- PostgreSQL.

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

Mudanças relevantes na arquitetura devem ser justificadas por ADR.

## Desenvolvimento

Priorizar contratos antes das implementações.

Quando outro componente ainda não estiver pronto, utilizar:

- mocks;
- interfaces;
- stubs;
- dados simulados.

Princípio:

> Depender de contratos e não da implementação de outro integrante.

Não criar microsserviços apenas para dividir o trabalho.

## Divisão da equipe

### Integrante 1 — Arquitetura
- C4 níveis 1, 2 e 3;
- visão arquitetural;
- revisão da consistência dos ADRs e diagramas.

### Integrante 2 — Mobile
- telas;
- navegação;
- estado;
- integração com API;
- mocks.

### Integrante 3 — Backend
- API em Java/Spring Boot;
- domínio;
- regras de negócio;
- testes;
- Repository;
- Strategy;
- Dependency Injection;
- integração com banco e mensageria.

### Integrante 4 — Dados
- modelagem;
- DER;
- PostgreSQL;
- migrations;
- seeds;
- ownership dos dados.

### Integrante 5 — Mensageria
- RabbitMQ;
- eventos;
- publisher;
- consumer;
- worker;
- retry;
- idempotência.

### Integrante 6 — Docker e Observabilidade
- Dockerfiles;
- Docker Compose;
- logs;
- métricas;
- traces;
- dashboards.

### Integrante 7 — CI/CD e Qualidade
- GitHub Actions;
- build;
- testes;
- lint;
- branch protection;
- Pull Requests.

## C4

O C4 deve ser criado antes da implementação e atualizado quando a arquitetura mudar.

Priorizar:

- Nível 1 — Contexto;
- Nível 2 — Containers;
- Nível 3 — Componentes principais do backend.

Nível 4 somente se necessário.

## ADRs

Cada responsável técnico deve criar os ADRs relacionados às decisões da própria área.

Fluxo esperado:

```text
Problema
  ↓
Alternativas
  ↓
Decisão
  ↓
ADR
  ↓
Implementação
```

Os ADRs devem ficar em:

```text
docs/adr/
```

## Git

O projeto utilizará duas branches principais:

```text
main
develop
```

### `develop`

A branch `develop` será a branch principal de integração durante o desenvolvimento.

Todas as funcionalidades, correções, documentações e alterações de infraestrutura deverão ser integradas primeiro em `develop`.

Nenhum integrante deve trabalhar diretamente em `develop`.

Fluxo:

```text
branch de trabalho
      ↓
Pull Request
      ↓
develop
```

### `main`

A branch `main` representa a versão estável e final do projeto.

Durante o desenvolvimento normal, não realizar merges de funcionalidades diretamente em `main`.

A migração de `develop` para `main` deverá ocorrer somente quando:

- funcionalidades estiverem concluídas;
- integrações estiverem funcionando;
- testes estiverem passando;
- documentação estiver atualizada;
- diagramas estiverem revisados;
- ADRs estiverem consistentes;
- Docker Compose estiver funcional;
- pipeline estiver verde.

Fluxo final:

```text
branches de trabalho
        ↓
      develop
        ↓
   validação final
        ↓
   Pull Request
        ↓
       main
```

## Branches de trabalho

Utilizar:

```text
feat/<issue>-<descricao>
fix/<issue>-<descricao>
infra/<issue>-<descricao>
docs/<issue>-<descricao>
test/<issue>-<descricao>
refactor/<issue>-<descricao>
```

Exemplos:

```text
feat/12-create-booking
infra/18-rabbitmq
infra/22-observability
docs/25-c4-container
fix/30-booking-validation
```

Toda branch de trabalho deve ser criada a partir da versão mais recente da `develop`.

```bash
git checkout develop
git pull origin develop
git checkout -b feat/12-create-booking
```

## Commits

Utilizar mensagens claras, preferencialmente seguindo Conventional Commits:

```text
feat: add booking creation endpoint
fix: prevent duplicated booking
docs: add messaging ADR
infra: configure RabbitMQ
test: add booking service tests
refactor: extract event publisher
```

## Backend Java

A estrutura sugerida é:

```text
services/api/
└── src/
    ├── main/
    │   ├── java/
    │   │   └── .../
    │   │       ├── controller/
    │   │       ├── service/
    │   │       ├── domain/
    │   │       ├── repository/
    │   │       ├── messaging/
    │   │       └── config/
    │   └── resources/
    │       └── application.yml
    └── test/
```

Preferir separação entre:

```text
Controller
   ↓
Service
   ↓
Domain
   ↓
Repository
```

Integrações externas, mensageria e persistência devem ficar desacopladas das regras de negócio sempre que possível.

## Design Patterns

### Repository Pattern

```text
BookingService
      |
      v
BookingRepository
```

Objetivo:

> desacoplar regra de negócio da persistência.

### Strategy Pattern

Pode ser utilizado para regras diferentes de cálculo.

```text
PriceStrategy
   |
   +--- WalkPriceStrategy
   +--- HostingPriceStrategy
   +--- CompanionPriceStrategy
```

### Dependency Injection

Utilizar injeção de dependência do Spring.

Preferir injeção por construtor.

Exemplo conceitual:

```java
@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }
}
```

Evitar dependências instanciadas manualmente dentro das classes de negócio.

## Mensageria

Utilizar RabbitMQ com Spring AMQP.

Eventos sugeridos:

```text
BookingCreated
BookingAccepted
BookingRejected
BookingCancelled
```

Cada evento deve possuir, quando aplicável:

```text
eventId
eventType
version
occurredAt
data
```

Fluxo esperado:

```text
Spring Boot API
      |
      v
RabbitMQ
      |
      v
Java Worker / Consumer
```

O consumidor deve considerar idempotência para evitar processamento duplicado.

## Banco de dados

Utilizar PostgreSQL com Spring Data JPA.

Entidades sugeridas:

```text
Tutor
Pet
Caregiver
Booking
```

Utilizar migrations, preferencialmente com:

```text
Flyway
```

ou ferramenta equivalente definida em ADR.

## Testes

No backend Java, utilizar preferencialmente:

- JUnit;
- Mockito;
- Spring Boot Test.

Cada funcionalidade relevante deve possuir testes compatíveis com sua responsabilidade.

## Pull Requests

Toda alteração relevante deve passar por Pull Request.

Durante o desenvolvimento:

```text
branch de trabalho → develop
```

Na entrega final:

```text
develop → main
```

Os Pull Requests devem informar:

- o que foi alterado;
- Issue relacionada;
- como testar;
- evidências quando necessário;
- possíveis impactos em outros módulos.

## Branch Protection

Configurar proteção em:

```text
develop
main
```

Regras recomendadas:

- impedir push direto;
- exigir Pull Request;
- exigir pelo menos uma aprovação;
- exigir CI verde;
- impedir merge com testes quebrados.

A `main` deve possuir proteção mais rígida por representar a versão estável do projeto.

## Regras para IA

Antes de implementar uma tarefa:

1. leia este arquivo;
2. consulte os ADRs relacionados;
3. consulte os contratos existentes;
4. confirme que está trabalhando em uma branch criada a partir de `develop`;
5. preserve a arquitetura definida;
6. altere somente o necessário para a tarefa.

A IA nunca deve orientar alterações diretamente em `main`.

Durante o desenvolvimento, o destino padrão dos Pull Requests deve ser:

```text
develop
```

A `main` somente deverá receber alterações a partir de `develop` na etapa final do projeto.

A implementação deve:

- ser testável;
- evitar acoplamento desnecessário;
- utilizar interfaces quando apropriado;
- utilizar injeção de dependência;
- permitir mocks;
- não adicionar credenciais;
- respeitar contratos existentes;
- atualizar documentação quando necessário.

Ao finalizar, informar:

- arquivos criados;
- arquivos modificados;
- como executar;
- como testar;
- possíveis impactos arquiteturais.