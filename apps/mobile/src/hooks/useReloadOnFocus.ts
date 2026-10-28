import { useFocusEffect } from '@react-navigation/native';
import { useCallback, useRef } from 'react';

/** Recarrega os dados quando a tela volta a ficar em foco (ignora o primeiro foco, já carregado na montagem). */
export function useReloadOnFocus(reload: (asRefresh?: boolean) => Promise<void>): void {
  const firstFocus = useRef(true);
  useFocusEffect(
    useCallback(() => {
      if (firstFocus.current) {
        firstFocus.current = false;
        return;
      }
      void reload(true);
    }, [reload]),
  );
}
