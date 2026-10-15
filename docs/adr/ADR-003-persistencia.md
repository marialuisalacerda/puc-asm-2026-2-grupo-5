# ADR-003 — Estratégia de Persistência

## Status

Proposto

## Contexto

O sistema conecta tutores de pets a cuidadores e possui fluxos que exigem persistência de dados relacionados a tutores, pets, cuidadores e reservas.

A API principal será implementada em Java com Spring Boot e utilizará Spring Data JPA para acesso aos dados.

A solução precisa garantir:

- integridade dos dados;
- relacionamento consistente entre as entidades;
- evolução controlada do schema;
- reprodutibilidade do ambiente;
- versionamento das alterações estruturais;
- configuração externa de credenciais;
- suporte a testes automatizados;
- separação clara de ownership dos dados.

Também é necessário evitar que diferentes componentes da solução mantenham ou alterem o mesmo schema de forma independente.

## Decisão

Será utilizado **PostgreSQL** como banco de dados relacional principal da aplicação.

O acesso ao banco será realizado pela API Java/Spring Boot utilizando **Spring Data JPA**.

As alterações estruturais do banco serão versionadas com **Flyway**.

As migrations serão armazenadas em:

```text
services/api/src/main/resources/db/migration/
```

O diretório seguirá a convenção do Flyway, por exemplo:

```text
V1__create_initial_schema.sql
V2__add_booking_indexes.sql
V3__add_booking_constraints.sql
```

A documentação específica do banco será mantida em:

```text
docs/database/
```

O DER e demais diagramas relacionados ao modelo de dados serão mantidos em:

```text
docs/diagrams/
```

Artefatos de infraestrutura relacionados ao PostgreSQL poderão ser mantidos em:

```text
infrastructure/database/
```

Esse diretório não deverá conter cópias das migrations do Flyway.

As migrations localizadas em `services/api/src/main/resources/db/migration/` serão a única fonte versionada de evolução do schema.

## Ownership dos dados

A API será responsável pela persistência dos dados do domínio principal.

Outros componentes, como o `notification-worker`, não deverão acessar diretamente as tabelas da API.

A comunicação entre componentes independentes deverá ocorrer por contratos definidos, como APIs ou eventos.

Essa regra evita acoplamento direto ao schema e reduz dependências entre componentes.

## Modelo inicial

O modelo inicial deverá contemplar, no mínimo:

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

As entidades, atributos, cardinalidades, constraints e índices serão detalhados durante a etapa de modelagem e registrados na documentação do banco e no DER.

## Migrations

Toda alteração estrutural no banco deverá ser realizada por migration.

Não deverão ser consideradas válidas alterações feitas manualmente diretamente no banco sem que exista uma migration correspondente no repositório.

As migrations deverão:

- possuir versionamento incremental;
- ter nomes descritivos;
- ser imutáveis após terem sido aplicadas e compartilhadas;
- evitar operações destrutivas sem justificativa;
- manter compatibilidade com a aplicação sempre que possível.

## Seeds

Dados iniciais poderão ser adicionados quando forem necessários para desenvolvimento ou demonstração.

Seeds não deverão conter:

- credenciais reais;
- dados pessoais reais;
- tokens;
- informações sensíveis.

Quando forem necessários dados específicos apenas para testes automatizados, eles deverão permanecer no contexto de testes e não nas migrations de produção.

## Constraints

O banco deverá utilizar constraints para proteger invariantes que pertençam à camada de persistência.

Devem ser avaliadas, conforme o modelo:

- `PRIMARY KEY`;
- `FOREIGN KEY`;
- `NOT NULL`;
- `UNIQUE`;
- `CHECK`.

Regras puramente de negócio continuarão sendo responsabilidade da camada de domínio/aplicação.

## Índices

Índices deverão ser criados com base nos padrões reais de consulta da aplicação.

Devem ser priorizados campos utilizados frequentemente em:

- filtros;
- relacionamentos;
- buscas;
- ordenações;
- joins.

Índices não deverão ser adicionados sem necessidade identificada, pois também aumentam custo de escrita e armazenamento.

## Configuração

Credenciais e strings de conexão não deverão ser versionadas no repositório.

A configuração deverá utilizar variáveis de ambiente ou mecanismos equivalentes suportados pelo Spring Boot.

Exemplos de informações externas à aplicação:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
```

Arquivos de exemplo poderão utilizar apenas valores fictícios ou locais.

## Testes

A estratégia de persistência deverá ser validada por testes automatizados.

Os testes deverão verificar, quando aplicável:

- inicialização do contexto Spring;
- aplicação das migrations;
- criação correta do schema;
- persistência das entidades;
- relacionamentos;
- constraints;
- consultas dos repositories;
- comportamento diante de dados inválidos;
- integração entre JPA e PostgreSQL.

A implementação poderá utilizar:

- JUnit;
- Spring Boot Test;
- testes de repository;
- PostgreSQL em ambiente isolado de testes, preferencialmente por container quando essa infraestrutura estiver disponível.

Os testes específicos serão definidos junto com a implementação das entidades e repositories.

## Alternativas consideradas

### MySQL

Banco relacional maduro e amplamente utilizado.

Não foi escolhido porque o projeto já definiu PostgreSQL como tecnologia principal e não existe requisito que justifique uma segunda tecnologia relacional.

### SQLite

Apresenta configuração simples e baixo custo para ambientes locais.

Não foi escolhido como banco principal porque o projeto prevê execução de backend em containers, relacionamentos transacionais e um ambiente mais próximo de uma aplicação distribuída.

### Alterações manuais sem ferramenta de migration

Reduzem o trabalho inicial, porém dificultam rastreabilidade, reprodução do ambiente e integração entre integrantes.

Por esse motivo, essa alternativa foi rejeitada.

## Consequências

### Positivas

- schema versionado junto com o código;
- histórico das alterações estruturais;
- maior reprodutibilidade entre ambientes;
- integração natural com Spring Boot;
- suporte a integridade referencial e transações;
- facilidade de revisão das alterações por Pull Request;
- menor risco de divergência entre bancos locais dos integrantes.

### Negativas

- toda alteração estrutural exige uma nova migration;
- migrations já compartilhadas não deverão ser alteradas;
- mudanças incompatíveis exigirão planejamento de migração;
- testes de integração com PostgreSQL possuem custo maior que testes puramente unitários.

## Estrutura relacionada

```text
services/api/
└── src/
    ├── main/
    │   └── resources/
    │       └── db/
    │           └── migration/
    └── test/

infrastructure/
└── database/

docs/
├── adr/
│   └── ADR-003-persistencia.md
├── database/
└── diagrams/
```

## Critérios de validação

Esta decisão será considerada corretamente implementada quando:

- PostgreSQL estiver configurado para o ambiente local;
- Flyway estiver integrado à API;
- migrations forem aplicadas automaticamente;
- schema inicial estiver versionado;
- DER estiver documentado;
- constraints estiverem implementadas;
- índices necessários estiverem documentados e versionados;
- credenciais não estiverem armazenadas no Git;
- testes de persistência estiverem passando;
- a documentação em `docs/database/` estiver atualizada.
