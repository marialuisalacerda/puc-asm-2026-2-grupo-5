import { useCallback, useEffect, useRef, useState } from 'react';

import { toUserMessage } from '../api/ApiError';

export interface AsyncState<T> {
  data: T | undefined;
  loading: boolean;
  refreshing: boolean;
  error: string | null;
  /** Recarrega; `asRefresh` ativa o indicador de "puxar para atualizar". Resolve quando os dados chegam. */
  reload(asRefresh?: boolean): Promise<void>;
}

interface Settled<T> {
  loader: () => Promise<T>;
  version: number;
  data: T | undefined;
  error: string | null;
}

/**
 * Carrega dados de forma assíncrona, com estado de carregamento e erro amigável.
 * `loader` deve ser estável (useCallback); quando ele muda, os dados são recarregados.
 */
export function useAsync<T>(loader: () => Promise<T>): AsyncState<T> {
  const [version, setVersion] = useState(0);
  const [refreshing, setRefreshing] = useState(false);
  const [settled, setSettled] = useState<Settled<T> | null>(null);
  const waiters = useRef<(() => void)[]>([]);

  useEffect(() => {
    let active = true;
    const finish = (next: Pick<Settled<T>, 'data' | 'error'>) => {
      if (!active) return;
      setSettled((prev) => ({
        loader,
        version,
        // Em caso de erro, mantém os últimos dados válidos na tela.
        data: next.error ? prev?.data : next.data,
        error: next.error,
      }));
      setRefreshing(false);
      const pending = waiters.current;
      waiters.current = [];
      pending.forEach((resolve) => resolve());
    };
    loader().then(
      (data) => finish({ data, error: null }),
      (e: unknown) => finish({ data: undefined, error: toUserMessage(e) }),
    );
    return () => {
      active = false;
    };
  }, [loader, version]);

  const reload = useCallback(
    (asRefresh = false) =>
      new Promise<void>((resolve) => {
        waiters.current.push(resolve);
        if (asRefresh) setRefreshing(true);
        setVersion((v) => v + 1);
      }),
    [],
  );

  const isCurrent = settled !== null && settled.loader === loader && settled.version === version;
  return {
    data: settled?.data,
    loading: !isCurrent && !refreshing,
    refreshing,
    error: settled?.error ?? null,
    reload,
  };
}
