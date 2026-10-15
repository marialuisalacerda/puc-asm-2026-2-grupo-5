# Banco de Dados

Esta pasta contém a documentação relacionada à camada de persistência do projeto.

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

## Versionamento

Toda alteração estrutural do banco deverá ser realizada por migrations.

Alterações manuais diretamente no banco não serão consideradas parte oficial da aplicação.

## Ambientes

Inicialmente serão considerados:

- desenvolvimento local;
- testes automatizados;
- ambiente de execução da aplicação.

As credenciais deverão ser fornecidas por variáveis de ambiente.

Nenhuma senha ou string de conexão real deverá ser versionada no Git.