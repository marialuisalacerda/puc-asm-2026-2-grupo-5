import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';

import type { Caregiver, Tutor } from '../api/types';

/** Login simulado: o usuário escolhe um perfil na tela de entrada, sem senha. */
export type Session = { role: 'TUTOR'; tutor: Tutor } | { role: 'CAREGIVER'; caregiver: Caregiver };

interface SessionContextValue {
  session: Session | null;
  signInAsTutor(tutor: Tutor): void;
  signInAsCaregiver(caregiver: Caregiver): void;
  signOut(): void;
}

const SessionContext = createContext<SessionContextValue | null>(null);

export function SessionProvider({ children, initialSession = null }: { children: ReactNode; initialSession?: Session | null }) {
  const [session, setSession] = useState<Session | null>(initialSession);

  const signInAsTutor = useCallback((tutor: Tutor) => setSession({ role: 'TUTOR', tutor }), []);
  const signInAsCaregiver = useCallback((caregiver: Caregiver) => setSession({ role: 'CAREGIVER', caregiver }), []);
  const signOut = useCallback(() => setSession(null), []);

  const value = useMemo(
    () => ({ session, signInAsTutor, signInAsCaregiver, signOut }),
    [session, signInAsTutor, signInAsCaregiver, signOut],
  );
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionContextValue {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error('useSession deve ser usado dentro de <SessionProvider>.');
  return ctx;
}

export function useTutorSession(): Tutor {
  const { session } = useSession();
  if (session?.role !== 'TUTOR') throw new Error('Tela disponível apenas para tutores.');
  return session.tutor;
}

export function useCaregiverSession(): Caregiver {
  const { session } = useSession();
  if (session?.role !== 'CAREGIVER') throw new Error('Tela disponível apenas para cuidadores.');
  return session.caregiver;
}
