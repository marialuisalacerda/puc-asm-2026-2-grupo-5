import { useEffect, useState } from 'react';

import { useApi } from '../api/ApiContext';
import type { Booking, PlantaoPetApi } from '../api/types';

export interface NameMaps {
  pets: Record<string, string>;
  caregivers: Record<string, string>;
  tutors: Record<string, string>;
}

type Entity = keyof NameMaps;

const fetchers: Record<Entity, { idOf(b: Booking): string; nameOf(api: PlantaoPetApi, id: string): Promise<string> }> = {
  pets: { idOf: (b) => b.petId, nameOf: async (api, id) => (await api.getPet(id)).name },
  caregivers: { idOf: (b) => b.caregiverId, nameOf: async (api, id) => (await api.getCaregiver(id)).name },
  tutors: { idOf: (b) => b.tutorId, nameOf: async (api, id) => (await api.getTutor(id)).name },
};

/**
 * Resolve nomes de pets, cuidadores e tutores das reservas (o contrato devolve apenas IDs).
 * Falhas individuais são ignoradas; a tela exibe "—" no lugar do nome.
 */
export function useNameLookup(bookings: Booking[] | undefined, include: Entity[]): NameMaps {
  const api = useApi();
  const [names, setNames] = useState<NameMaps>({ pets: {}, caregivers: {}, tutors: {} });
  const includeKey = include.join(',');

  useEffect(() => {
    if (!bookings?.length) return;
    let active = true;
    const entities = includeKey.split(',') as Entity[];

    Promise.all(
      entities.map(async (entity) => {
        const { idOf, nameOf } = fetchers[entity];
        const ids = [...new Set(bookings.map(idOf))];
        const entries = await Promise.all(
          ids.map(async (id) => [id, await nameOf(api, id).catch(() => '—')] as const),
        );
        return [entity, Object.fromEntries(entries)] as const;
      }),
    ).then((results) => {
      if (!active) return;
      setNames((prev) => {
        const next = { ...prev };
        for (const [entity, map] of results) next[entity] = { ...prev[entity], ...map };
        return next;
      });
    });

    return () => {
      active = false;
    };
  }, [api, bookings, includeKey]);

  return names;
}
