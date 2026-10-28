# C4 · Nível 1 — Contexto do sistema

> Índice do C4: [README](README.md) · Próximo nível: [C2 — Containers](C2-containers.md)

## Objetivo

Mostrar **quem usa** o Plantão de Pet e **com quais sistemas externos** ele se relaciona, sem detalhes de tecnologia.

O Plantão de Pet é um marketplace que conecta **tutores de cães** a **cuidadores** que oferecem passeio, companhia, visita domiciliar, hospedagem e cuidado temporário. O tutor solicita uma reserva; o cuidador aceita ou rejeita; ambos são notificados das mudanças.

## Diagrama

```mermaid
flowchart TB
    tutor["👤 Tutor<br/><i>[Pessoa]</i><br/>Cadastra pets, busca cuidadores,<br/>solicita e cancela reservas"]
    cuidador["👤 Cuidador<br/><i>[Pessoa]</i><br/>Aceita, rejeita ou cancela<br/>reservas recebidas"]
    suporte["👤 Suporte<br/><i>[Pessoa · fora do MVP]</i><br/>Atende tutores e cuidadores"]

    sistema["<b>Plantão de Pet</b><br/><i>[Sistema de software]</i><br/>Gerencia tutores, pets, cuidadores,<br/>reservas, preço e notificações"]

    pagamento["Gateway de pagamento<br/><i>[Sistema externo · fora do MVP]</i><br/>Cobrança, split e reembolso"]
    mensagens["Provedores de e-mail, SMS e push<br/><i>[Sistema externo · fora do MVP]</i><br/>Entrega das notificações"]

    tutor -->|"Usa o app para pets e reservas"| sistema
    cuidador -->|"Usa o app para responder reservas"| sistema
    suporte -.->|"Consulta e corrige dados (futuro)"| sistema
    sistema -.->|"Cobra e reembolsa reservas (futuro)"| pagamento
    sistema -.->|"Envia notificações (futuro)"| mensagens
    mensagens -.->|"Push, e-mail ou SMS"| tutor
    mensagens -.->|"Push, e-mail ou SMS"| cuidador

    classDef person fill:#08427b,stroke:#052e56,color:#ffffff
    classDef system fill:#1168bd,stroke:#0b4884,color:#ffffff
    classDef future fill:#999999,stroke:#6b6b6b,color:#ffffff,stroke-dasharray: 5 5
    class tutor,cuidador person
    class suporte future
    class sistema system
    class pagamento,mensagens future
```

Legenda: azul-escuro = pessoa; azul = o sistema; cinza tracejado = **fora do MVP** (previsto, não implementado).

## Elementos

| Elemento | Tipo | Responsabilidade | No MVP? |
|---|---|---|---|
| Tutor | Pessoa | Dono do cão. Cadastra pets, consulta cuidadores, solicita, acompanha e cancela reservas | Sim |
| Cuidador | Pessoa | Presta o serviço. Aceita, rejeita ou cancela reservas | Sim |
| Suporte | Pessoa | Atendimento e correção de dados | Não |
| Plantão de Pet | Sistema | Regras de reserva, cálculo de preço, persistência e notificações | Sim |
| Gateway de pagamento | Sistema externo | Cobrança no app, split e reembolso | Não |
| Provedores de e-mail/SMS/push | Sistema externo | Entrega real das notificações | Não |

## Decisões de escopo refletidas no diagrama

- **Notificações no MVP:** o sistema **gera e registra** as notificações (banco do `notification-worker`), mas não as entrega por um provedor externo. A troca do registro por um provedor real fica atrás de uma interface no worker e não muda este nível.
- **Pagamento no MVP:** o preço é calculado e gravado na reserva (`priceCents`), mas não há cobrança. O ciclo de estados do MVP não tem `PAID` (ver [estados da reserva](../diagrams/estados-reserva.md)).
- **Identidade:** não há autenticação no MVP; tutor e cuidador são identificados pelos IDs de demonstração do seed ([contratos de integração](../architecture/contratos-integracao.md)).

## Versão em imagem

A primeira versão do C1, elaborada pelo responsável pela arquitetura, está em imagem:

![C1 — Contexto do Sistema Plantão de Pet](C4%20Architecture%20Model%20-%20C1%20-%20Contexto%20do%20Sistema%20Plant%C3%A3o%20de%20Pet.jpg)

O diagrama Mermaid acima é a versão **mantida junto ao código** (revisável por Pull Request). Se houver divergência, vale o Mermaid, e a imagem deve ser regerada.
