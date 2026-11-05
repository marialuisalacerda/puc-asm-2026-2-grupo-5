# Modelo C4 — Plantão de Pet

Modelagem da arquitetura em níveis de abstração ([c4model.com](https://c4model.com)). Os diagramas são escritos em **Mermaid**, renderizado pelo próprio GitHub, para que sejam versionados e revisados por Pull Request junto com o código.

## Níveis

| Nível | Arquivo | Pergunta que responde | Público |
|---|---|---|---|
| 1 · Contexto | [C1-contexto.md](C1-contexto.md) | Quem usa o sistema e com quais sistemas externos ele fala? | todos |
| 2 · Containers | [C2-containers.md](C2-containers.md) | Quais unidades executáveis e bancos existem, com que tecnologia e protocolo? Quem é dono de cada dado? | equipe, avaliadores |

Imagem original do C1: [C4 Architecture Model - C1 - Contexto do Sistema Plantão de Pet.jpg](C4%20Architecture%20Model%20-%20C1%20-%20Contexto%20do%20Sistema%20Plant%C3%A3o%20de%20Pet.jpg).

### Diagramas complementares (`docs/diagrams/`)

| Diagrama | Arquivo |
|---|---|
| Deployment (host Docker, rede, volumes, portas) | [deployment.md](../diagrams/deployment.md) |
| DER (modelo de dados da API) | [DER-inicial.md](../diagrams/DER-inicial.md) |

## Por que não há nível 4 (código)

O C4 recomenda o nível 4 apenas quando ele acrescenta algo que o código não mostra. Aqui ele não é necessário porque:

- as estruturas de dados estão no [DER](../diagrams/DER-inicial.md);
- o comportamento dinâmico está nos diagramas de sequência e de estados;
- um diagrama de classes desatualiza a cada refatoração, e o próprio código (e a IDE) é a fonte mais confiável nesse nível.

Se surgir um trecho de lógica difícil de entender só pelo código, ele ganha um diagrama pontual em `docs/diagrams/`, não um nível 4 completo.

## Convenções

- **Notação:** `flowchart` com estilo C4 (pessoa, sistema, container, componente), `sequenceDiagram` e `stateDiagram-v2`. A sintaxe experimental `C4Context` do Mermaid não é usada por renderizar de forma instável no GitHub.
- **Cores:** azul-escuro = pessoa; azul = sistema ou container da aplicação; verde = banco de dados; cinza = observabilidade, elemento externo ou vizinho; tracejado = fora do MVP ou uso secundário.
- **Rótulo de cada caixa:** nome, `[tipo: tecnologia]` e responsabilidade em uma frase.
- **Setas:** sempre com verbo e protocolo (`HTTP/JSON`, `AMQP`, `JDBC`, `OTLP`). Sentido = quem inicia a comunicação.
- **Nomes:** os mesmos do `docker-compose.yml` e dos pacotes Java. Se um nome mudar no código, muda aqui no mesmo PR.
- **Idioma:** português; nomes técnicos (classes, filas, rotas) como estão no código.

## Quando atualizar

| Mudança | Atualizar |
|---|---|
| Novo ator ou sistema externo (ex.: entrar o gateway de pagamento) | C1 e C2 |
| Novo container, banco, fila ou serviço no `docker-compose.yml` | C2 e [deployment](../diagrams/deployment.md) |
| Mudança de protocolo ou de dono de um dado | C2 |

Regras:

1. Mudança relevante de arquitetura exige **ADR** em `docs/adr/` (seção "Impacto no C4") e a atualização do diagrama no **mesmo Pull Request**.
2. O responsável pela arquitetura revisa a consistência entre ADRs, contratos e diagramas antes do merge em `develop`.
3. Antes do merge `develop → main`, todos os níveis devem refletir o que roda no `docker compose --profile app up -d`.
