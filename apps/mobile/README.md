# Plantão de Pet — app mobile

Aplicativo React Native (Expo SDK 57 + TypeScript) para tutores de cães e cuidadores.
Decisão de stack: [ADR-002](../../docs/adr/ADR-002-stack-mobile.md). Contrato consumido: [OpenAPI](../../docs/api/openapi.yaml).
Regras, preços e dados de demonstração: [contratos de integração](../../docs/architecture/contratos-integracao.md).

## Funcionalidades

| Perfil | Tela | O que faz |
|---|---|---|
| — | Entrar | Escolhe o perfil (Ana Tutora, Bruno Tutor ou um cuidador ativo). Login simulado, sem senha. |
| Tutor | Meus pets | Lista e cadastra pets (nome, porte, observações; espécie `DOG`). |
| Tutor | Cuidadores → Nova reserva | Escolhe pet, serviço, dia/horário/duração predefinidos, vê o orçamento (`GET /bookings/quote`) e confirma (`POST /bookings`). |
| Tutor | Minhas reservas | Status traduzido e colorido, puxar para atualizar, cancelar reservas Solicitadas/Aceitas. |
| Cuidador | Solicitações | Aceita ou recusa reservas Solicitadas; histórico das demais. |

Erros da API (Problem JSON, RFC 7807) viram mensagens amigáveis, por exemplo 409 na criação: "Cuidador já tem reserva nesse horário."

## Pré-requisitos

- Node.js 20.19.4 ou superior (o CI usa Node 20) e npm.
- Para rodar no aparelho: app **Expo Go** (compatível com o SDK 57) ou emulador Android/simulador iOS. Para o navegador, basta `npm run web`.

## Como rodar

```bash
cd apps/mobile
npm ci
cp .env.example .env     # opcional; sem .env o app usa mocks
npm start                # abre o Metro; pressione a (Android), i (iOS) ou w (web)
```

### Com dados simulados (padrão)

`EXPO_PUBLIC_USE_MOCKS=true` (ou variável ausente) usa `MockPlantaoPetApi`: dados em memória com os mesmos IDs e nomes do seed
`V4__seed_demo_data.sql`, as mesmas regras de negócio e a mesma tabela de preços, com latência simulada. Os dados voltam ao
estado inicial quando o app é recarregado.

### Contra a API local

1. Na raiz do repositório, suba a API e as dependências:

   ```bash
   docker compose --profile app up -d
   ```

2. Em `apps/mobile/.env`:

   ```dotenv
   EXPO_PUBLIC_USE_MOCKS=false
   EXPO_PUBLIC_API_URL=http://localhost:8080
   ```

   | Onde o app roda | `EXPO_PUBLIC_API_URL` |
   |---|---|
   | Navegador (`npm run web`) ou simulador iOS | `http://localhost:8080` |
   | Emulador Android | `http://10.0.2.2:8080` |
   | Celular físico (mesma rede Wi-Fi) | `http://<IP-da-sua-máquina>:8080` |

3. Reinicie o Metro limpando o cache para aplicar as variáveis: `npx expo start -c`.

A tela de entrada mostra se o app está em modo demonstração ou qual URL de API está usando.

> Na web, a API precisa permitir CORS para a origem do Metro (`http://localhost:8081`). No Android/iOS isso não se aplica.
> Como a API local usa HTTP, builds Android de produção exigiriam liberar tráfego em texto claro; com Expo Go em desenvolvimento funciona.

## Estrutura

```text
apps/mobile/
├── App.tsx                    # Providers (SafeArea, ApiProvider, SessionProvider) + navegação
├── index.ts                   # registerRootComponent
├── src/
│   ├── api/
│   │   ├── types.ts           # tipos do OpenAPI + interface PlantaoPetApi (porta)
│   │   ├── ApiError.ts        # erro tipado a partir do Problem (RFC 7807) + mensagens amigáveis
│   │   ├── HttpPlantaoPetApi.ts  # implementação HTTP (fetch injetável)
│   │   ├── mock/              # MockPlantaoPetApi, seeds do contrato, tabela de preços
│   │   ├── createApi.ts       # fábrica: mock ou HTTP conforme EXPO_PUBLIC_USE_MOCKS
│   │   └── ApiContext.tsx     # injeção de dependência via React Context (useApi)
│   ├── config/env.ts          # leitura de EXPO_PUBLIC_API_URL / EXPO_PUBLIC_USE_MOCKS
│   ├── state/SessionContext.tsx  # sessão (perfil escolhido) com Context + hooks
│   ├── hooks/                 # useAsync, useNameLookup, useReloadOnFocus
│   ├── navigation/            # native-stack + bottom-tabs (tutor)
│   ├── screens/               # EntryScreen, tutor/*, caregiver/*
│   ├── components/            # botões, chips, status, cartões
│   └── utils/                 # formatação BRL/pt-BR, rótulos PT, predefinições de agenda
└── **/__tests__/              # testes Jest
```

As telas dependem apenas da interface `PlantaoPetApi` (obtida por `useApi()`), nunca de uma implementação concreta.
Para trocar de implementação basta a variável de ambiente; nos testes, passa-se a implementação ao `ApiProvider`.

## Como testar

```bash
cd apps/mobile
npx tsc --noEmit         # tipos
npm run lint             # ESLint (eslint-config-expo)
npm test -- --ci         # Jest (jest-expo)
```

São os mesmos passos do workflow `mobile-ci` (ADR-007). Cobertura atual:

- `MockPlantaoPetApi`: seeds, sobreposição (409), transições válidas/inválidas, 400/404/422, preços por serviço (ex.: `WALK` 2h = 6000).
- `HttpPlantaoPetApi` com `fetch` simulado: URLs, métodos, query, corpo JSON, conversão de Problem em `ApiError`, falha de rede.
- Utilitários: moeda BRL, datas pt-BR, durações, predefinições de agenda, leitura das variáveis de ambiente.
- Componente: tela de entrada lista os perfis do seed e inicia a sessão.

## Limitações conhecidas

- Sem autenticação real (fora do escopo do contrato atual).
- Datas são escolhidas por predefinições (dia + horário + duração), sem date-picker nativo.
- O contrato só devolve IDs nas reservas; os nomes de pet, tutor e cuidador são resolvidos com chamadas `GET` adicionais.
