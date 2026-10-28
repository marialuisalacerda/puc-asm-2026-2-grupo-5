# Banco de Dados

Esta pasta contém a documentação relacionada à camada de persistência do projeto.

A decisão de usar PostgreSQL, Spring Data JPA e Flyway, e a regra de que somente a API acessa o banco, estão registradas no [ADR-003](../adr/ADR-003-persistencia.md). Este documento descreve como aplicar essa decisão no dia a dia e pode ser atualizado sem precisar de um novo ADR.

## Objetivos

A estratégia de persistência deverá:

- garantir integridade dos dados;
- permitir evolução controlada do schema;
- permitir execução do ambiente local;
- possuir versionamento das alterações do banco;
- permitir testes automatizados;
- manter separadas as configurações dos diferentes ambientes;
- evitar armazenamento de credenciais no repositório.

## Estrutura

```text
services/api/
└── src/
    └── main/
        └── resources/
            └── db/
                └── migration/      # migrations do Flyway; única fonte do schema

infrastructure/
└── database/
    ├── docker-compose.yml          # PostgreSQL local enquanto o compose global não estiver integrado
    ├── .env.example
    └── tests/                      # testes isolados de schema/migrations

docs/
├── adr/
│   └── ADR-003-persistencia.md
├── database/
│   ├── README.MD
│   ├── modelo-logico-inicial.md
│   └── REVISAO-PACOTE-BANCO.md
└── diagrams/
    └── DER-inicial.md
```

## Modelo inicial

O modelo inicial contempla:

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

A proposta detalhada de atributos, cardinalidades, constraints e índices está em [modelo-logico-inicial.md](modelo-logico-inicial.md), e o DER está em [DER-inicial.md](../diagrams/DER-inicial.md).

Enquanto os contratos definitivos da API ainda estiverem em revisão, o modelo lógico deve ser tratado como proposta e validado com o responsável pelo backend antes de alterações incompatíveis.

## Versionamento

Toda alteração estrutural do banco deverá ser realizada por migrations.

Alterações manuais diretamente no banco não serão consideradas parte oficial da aplicação.

As migrations ficam em `services/api/src/main/resources/db/migration/` e seguem a convenção do Flyway:

```text
V1__create_initial_schema.sql
V2__add_relationship_indexes.sql
```

As migrations deverão:

- possuir versionamento incremental;
- ter nomes descritivos;
- ser imutáveis após terem sido aplicadas e compartilhadas;
- evitar operações destrutivas sem justificativa;
- manter compatibilidade com a aplicação sempre que possível.

## Dados iniciais (seeds)

Dados iniciais poderão ser adicionados quando forem necessários para desenvolvimento ou demonstração.

Seeds não deverão conter credenciais reais, dados pessoais reais, tokens ou informações sensíveis.

Dados necessários apenas para testes automatizados deverão permanecer no contexto de testes, e não nas migrations de produção.

## Constraints

O banco deverá utilizar constraints para proteger invariantes que pertençam à camada de persistência. Devem ser avaliadas, conforme o modelo:

- `PRIMARY KEY`;
- `FOREIGN KEY`;
- `NOT NULL`;
- `UNIQUE`;
- `CHECK`.

Regras puramente de negócio continuarão sendo responsabilidade da camada de domínio/aplicação.

## Índices

Índices deverão ser criados com base nos padrões reais de consulta, priorizando campos usados com frequência em filtros, relacionamentos, buscas, ordenações e joins.

A migration `V2__add_relationship_indexes.sql` adiciona apenas índices das colunas de relacionamento inicialmente definidas. Índices adicionais deverão ser justificados pelos contratos e consultas implementados.

## Ambientes

Inicialmente serão considerados:

- desenvolvimento local;
- testes automatizados;
- ambiente de execução da aplicação.

As credenciais deverão ser fornecidas por variáveis de ambiente.

Nenhuma senha ou string de conexão real deverá ser versionada no Git.

Variáveis esperadas:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
```

O arquivo `infrastructure/database/.env.example` contém somente valores locais/fictícios.

## Ambiente PostgreSQL local

Enquanto o Docker Compose global do projeto não estiver integrado, o banco pode ser iniciado isoladamente:

```bash
docker compose -f infrastructure/database/docker-compose.yml up -d
```

Este compose deverá posteriormente ser integrado ao compose raiz pelo responsável de infraestrutura, evitando configurações duplicadas.

## Testes

A suíte independente em `infrastructure/database/tests/` valida as migrations reais em um PostgreSQL temporário criado com Testcontainers.

Executar, a partir da raiz do repositório:

```bash
mvn -f infrastructure/database/tests/pom.xml test
```

Pré-requisitos:

- Java 17+;
- Maven 3.9+;
- Docker em execução.

Os testes cobrem:

- aplicação das migrations;
- criação do schema;
- foreign keys;
- constraints de unicidade;
- constraints `CHECK`;
- política de exclusão atualmente definida;
- índices estruturais.

Quando a API Spring Boot estiver implementada, deverão ser adicionados também testes de repository e integração JPA/PostgreSQL no módulo da API.

## Checklist de implementação

A estratégia de persistência estará implementada quando:

- [x] PostgreSQL possuir configuração isolada para desenvolvimento local;
- [ ] PostgreSQL estiver integrado ao Docker Compose global;
- [ ] Flyway estiver integrado à API Spring Boot e as migrations forem aplicadas automaticamente pela aplicação;
- [x] schema inicial estiver versionado em migrations;
- [x] DER inicial estiver documentado;
- [x] constraints estruturais iniciais estiverem implementadas;
- [x] índices iniciais de relacionamento estiverem documentados e versionados;
- [x] nenhuma credencial real estiver armazenada no Git;
- [x] testes independentes de schema/migrations estiverem disponíveis;
- [ ] testes JPA/repository estiverem passando após implementação do backend;
- [ ] ADR-003 estiver revisado e aceito;
- [ ] modelo lógico estiver validado com o backend.
