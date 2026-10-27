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

## Estrutura planejada

A implementação do banco deverá contemplar:

- modelo conceitual;
- modelo lógico;
- modelo físico;
- migrations;
- configuração de ambiente local;
- dados iniciais quando necessários;
- testes de integração;
- documentação das entidades e relacionamentos.

## Estrutura de pastas

```text
services/api/
└── src/
    ├── main/
    │   └── resources/
    │       └── db/
    │           └── migration/      # migrations do Flyway (única fonte do schema)
    └── test/

infrastructure/
└── database/                       # artefatos de infra do PostgreSQL (sem cópias de migrations)

docs/
├── adr/
│   └── ADR-003-persistencia.md
├── database/                       # este documento e demais docs do banco
└── diagrams/                       # DER e diagramas do modelo de dados
```

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

Entidades, atributos, cardinalidades, constraints e índices serão detalhados durante a modelagem e registrados no DER (`docs/diagrams/`).

## Versionamento

Toda alteração estrutural do banco deverá ser realizada por migrations.

Alterações manuais diretamente no banco não serão consideradas parte oficial da aplicação.

As migrations ficam em `services/api/src/main/resources/db/migration/` e seguem a convenção do Flyway:

```text
V1__create_initial_schema.sql
V2__add_booking_indexes.sql
V3__add_booking_constraints.sql
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

- `PRIMARY KEY`
- `FOREIGN KEY`
- `NOT NULL`
- `UNIQUE`
- `CHECK`

Regras puramente de negócio continuarão sendo responsabilidade da camada de domínio/aplicação.

## Índices

Índices deverão ser criados com base nos padrões reais de consulta, priorizando campos usados com frequência em filtros, relacionamentos, buscas, ordenações e joins.

Índices não deverão ser adicionados sem necessidade identificada, pois aumentam o custo de escrita e armazenamento.

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

Arquivos de exemplo poderão utilizar apenas valores fictícios ou locais.

## Testes

Os testes de persistência deverão verificar, quando aplicável:

- inicialização do contexto Spring;
- aplicação das migrations e criação correta do schema;
- persistência das entidades e relacionamentos;
- constraints e comportamento diante de dados inválidos;
- consultas dos repositories;
- integração entre JPA e PostgreSQL.

Ferramentas: JUnit, Spring Boot Test, testes de repository e PostgreSQL isolado para testes, preferencialmente por container quando essa infraestrutura estiver disponível.

Os testes específicos serão definidos junto com a implementação das entidades e repositories.

## Checklist de implementação

A estratégia de persistência estará implementada quando:

- [ ] PostgreSQL estiver configurado para o ambiente local;
- [ ] Flyway estiver integrado à API e as migrations forem aplicadas automaticamente;
- [ ] o schema inicial estiver versionado;
- [ ] o DER estiver documentado;
- [ ] as constraints estiverem implementadas;
- [ ] os índices necessários estiverem documentados e versionados;
- [ ] não houver credenciais armazenadas no Git;
- [ ] os testes de persistência estiverem passando;
- [ ] esta documentação estiver atualizada.
