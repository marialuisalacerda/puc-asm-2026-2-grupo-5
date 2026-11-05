import type { Caregiver, Pet, Tutor } from '../types';

/**
 * Dados de demonstração com os MESMOS IDs e nomes do seed da API
 * (services/api/src/main/resources/db/migration/V4__seed_demo_data.sql
 * e docs/architecture/contratos-integracao.md). Dados fictícios.
 */
export const SEED_IDS = {
  tutors: {
    ana: '11111111-1111-1111-1111-111111111111',
    bruno: '11111111-1111-1111-1111-111111111112',
  },
  pets: {
    thor: '22222222-2222-2222-2222-222222222221',
    mel: '22222222-2222-2222-2222-222222222222',
    bidu: '22222222-2222-2222-2222-222222222223',
  },
  caregivers: {
    carla: '33333333-3333-3333-3333-333333333331',
    diego: '33333333-3333-3333-3333-333333333332',
    elisa: '33333333-3333-3333-3333-333333333333',
    fabio: '33333333-3333-3333-3333-333333333334',
  },
} as const;

export const SEED_TUTORS: Tutor[] = [
  { id: SEED_IDS.tutors.ana, name: 'Ana Tutora', email: 'ana.tutora@example.com' },
  { id: SEED_IDS.tutors.bruno, name: 'Bruno Tutor', email: 'bruno.tutor@example.com' },
];

export const SEED_PETS: Pet[] = [
  { id: SEED_IDS.pets.thor, tutorId: SEED_IDS.tutors.ana, name: 'Thor', species: 'DOG', size: 'LARGE', notes: null },
  { id: SEED_IDS.pets.mel, tutorId: SEED_IDS.tutors.ana, name: 'Mel', species: 'DOG', size: 'SMALL', notes: null },
  { id: SEED_IDS.pets.bidu, tutorId: SEED_IDS.tutors.bruno, name: 'Bidu', species: 'DOG', size: 'MEDIUM', notes: null },
];

export const SEED_CAREGIVERS: Caregiver[] = [
  { id: SEED_IDS.caregivers.carla, name: 'Carla Cuidadora', email: 'carla.cuidadora@example.com', active: true },
  { id: SEED_IDS.caregivers.diego, name: 'Diego Passeador', email: 'diego.passeador@example.com', active: true },
  { id: SEED_IDS.caregivers.elisa, name: 'Elisa Anfitriã', email: 'elisa.anfitria@example.com', active: true },
  { id: SEED_IDS.caregivers.fabio, name: 'Fábio Inativo', email: 'fabio.inativo@example.com', active: false },
];
