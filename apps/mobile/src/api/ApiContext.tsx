import { createContext, useContext, useMemo, type ReactNode } from 'react';

import { readConfig, type AppConfig } from '../config/env';
import { createApi } from './createApi';
import type { PlantaoPetApi } from './types';

interface ApiContextValue {
  api: PlantaoPetApi;
  config: AppConfig;
}

const ApiContext = createContext<ApiContextValue | null>(null);

interface ApiProviderProps {
  children: ReactNode;
  /** Permite injetar uma implementação (ex.: mock nos testes). */
  api?: PlantaoPetApi;
  config?: AppConfig;
}

/** Injeção de dependência da camada de dados via React Context. */
export function ApiProvider({ children, api, config }: ApiProviderProps) {
  const value = useMemo<ApiContextValue>(() => {
    const resolvedConfig = config ?? readConfig();
    return { api: api ?? createApi(resolvedConfig), config: resolvedConfig };
  }, [api, config]);
  return <ApiContext.Provider value={value}>{children}</ApiContext.Provider>;
}

export function useApiContext(): ApiContextValue {
  const ctx = useContext(ApiContext);
  if (!ctx) throw new Error('useApi deve ser usado dentro de <ApiProvider>.');
  return ctx;
}

export function useApi(): PlantaoPetApi {
  return useApiContext().api;
}
