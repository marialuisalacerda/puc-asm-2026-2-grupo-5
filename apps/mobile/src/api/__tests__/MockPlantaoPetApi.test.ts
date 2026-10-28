import { ApiError } from '../ApiError';
import { MockPlantaoPetApi } from '../mock/MockPlantaoPetApi';
import { calculatePriceCents } from '../mock/pricing';
import { SEED_IDS } from '../mock/seeds';
import type { CreateBookingRequest } from '../types';

const NOW = new Date('2026-10-28T12:00:00Z');
const { tutors, pets, caregivers } = SEED_IDS;

function newApi() {
  let seq = 0;
  return new MockPlantaoPetApi({
    latencyMs: 0,
    now: () => NOW,
    idFactory: () => `00000000-0000-4000-8000-${String(++seq).padStart(12, '0')}`,
  });
}

const walk = (overrides: Partial<CreateBookingRequest> = {}): CreateBookingRequest => ({
  tutorId: tutors.ana,
  petId: pets.thor,
  caregiverId: caregivers.carla,
  serviceType: 'WALK',
  scheduledStart: '2026-10-29T13:00:00Z',
  scheduledEnd: '2026-10-29T15:00:00Z',
  ...overrides,
});

async function expectApiError(promise: Promise<unknown>, status: number): Promise<ApiError> {
  try {
    await promise;
  } catch (e) {
    expect(e).toBeInstanceOf(ApiError);
    expect((e as ApiError).status).toBe(status);
    return e as ApiError;
  }
  throw new Error(`Esperava ApiError ${status}`);
}

describe('MockPlantaoPetApi — seeds do contrato', () => {
  it('expõe tutores, pets e cuidadores com os IDs fixos', async () => {
    const api = newApi();
    expect((await api.listTutors()).map((t) => t.name)).toEqual(['Ana Tutora', 'Bruno Tutor']);
    expect((await api.listTutorPets(tutors.ana)).map((p) => p.name)).toEqual(['Thor', 'Mel']);
    expect((await api.getPet(pets.bidu)).tutorId).toBe(tutors.bruno);
  });

  it('lista somente cuidadores ativos por padrão', async () => {
    const api = newApi();
    const active = await api.listCaregivers();
    expect(active.map((c) => c.name)).toEqual(['Carla Cuidadora', 'Diego Passeador', 'Elisa Anfitriã']);
    expect((await api.listCaregivers({ active: false })).map((c) => c.id)).toEqual([caregivers.fabio]);
  });

  it('cadastra pet para o tutor', async () => {
    const api = newApi();
    const pet = await api.createPet({ tutorId: tutors.bruno, name: ' Rex ', species: 'DOG', size: 'LARGE' });
    expect(pet).toMatchObject({ tutorId: tutors.bruno, name: 'Rex', species: 'DOG', size: 'LARGE', notes: null });
    expect((await api.listTutorPets(tutors.bruno)).map((p) => p.name)).toEqual(['Bidu', 'Rex']);
  });

  it('rejeita pet sem nome (400) e tutor inexistente (404)', async () => {
    const api = newApi();
    const err = await expectApiError(api.createPet({ tutorId: tutors.ana, name: '', species: 'DOG' }), 400);
    expect(err.problem.errors?.[0].field).toBe('name');
    await expectApiError(api.createPet({ tutorId: 'nao-existe', name: 'X', species: 'DOG' }), 404);
  });
});

describe('MockPlantaoPetApi — preço (Strategy)', () => {
  it('WALK de 2h custa 6000 centavos', async () => {
    const quote = await newApi().quoteBooking({
      serviceType: 'WALK',
      scheduledStart: '2026-10-29T13:00:00Z',
      scheduledEnd: '2026-10-29T15:00:00Z',
    });
    expect(quote).toMatchObject({ priceCents: 6000, currency: 'BRL', serviceType: 'WALK' });
  });

  it.each([
    ['WALK', '2026-10-29T13:00:00Z', '2026-10-29T14:30:00Z', 6000],
    ['WALK', '2026-10-29T13:00:00Z', '2026-10-29T13:20:00Z', 3000],
    ['COMPANIONSHIP', '2026-10-29T13:00:00Z', '2026-10-29T16:00:00Z', 7500],
    ['TEMPORARY_CARE', '2026-10-29T13:00:00Z', '2026-10-29T14:01:00Z', 4000],
    ['HOSTING', '2026-10-29T00:00:00Z', '2026-10-30T06:00:00Z', 24000],
    ['HOSTING', '2026-10-29T10:00:00Z', '2026-10-29T12:00:00Z', 12000],
    ['HOME_VISIT', '2026-10-29T10:00:00Z', '2026-10-29T11:00:00Z', 4500],
    ['HOME_VISIT', '2026-10-29T23:00:00Z', '2026-10-30T01:00:00Z', 9000],
  ] as const)('%s de %s a %s → %i', (type, start, end, expected) => {
    expect(calculatePriceCents(type, new Date(start), new Date(end))).toBe(expected);
  });

  it('orçamento com fim antes do início retorna 400', async () => {
    await expectApiError(
      newApi().quoteBooking({ serviceType: 'WALK', scheduledStart: '2026-10-29T15:00:00Z', scheduledEnd: '2026-10-29T13:00:00Z' }),
      400,
    );
  });
});

describe('MockPlantaoPetApi — criação de reserva', () => {
  it('cria reserva REQUESTED com preço calculado', async () => {
    const api = newApi();
    const booking = await api.createBooking(walk());
    expect(booking).toMatchObject({ status: 'REQUESTED', priceCents: 6000, createdAt: NOW.toISOString() });
    expect(await api.listBookings({ tutorId: tutors.ana })).toHaveLength(1);
    expect(await api.listBookings({ tutorId: tutors.bruno })).toHaveLength(0);
  });

  it('retorna 409 quando o cuidador já tem reserva sobreposta', async () => {
    const api = newApi();
    await api.createBooking(walk());
    const err = await expectApiError(
      api.createBooking(walk({ petId: pets.bidu, tutorId: tutors.bruno, scheduledStart: '2026-10-29T14:00:00Z', scheduledEnd: '2026-10-29T16:00:00Z' })),
      409,
    );
    expect(err.friendlyMessage).toBe('Cuidador já tem reserva nesse horário.');
  });

  it('permite horário adjacente e reserva sobre período de reserva cancelada', async () => {
    const api = newApi();
    const first = await api.createBooking(walk());
    await api.createBooking(walk({ scheduledStart: '2026-10-29T15:00:00Z', scheduledEnd: '2026-10-29T16:00:00Z' }));
    await api.cancelBooking(first.id);
    await expect(api.createBooking(walk({ petId: pets.mel }))).resolves.toMatchObject({ status: 'REQUESTED' });
  });

  it('aplica 422 para pet de outro tutor, cuidador inativo e início no passado', async () => {
    const api = newApi();
    await expectApiError(api.createBooking(walk({ petId: pets.bidu })), 422);
    await expectApiError(api.createBooking(walk({ caregiverId: caregivers.fabio })), 422);
    await expectApiError(
      api.createBooking(walk({ scheduledStart: '2026-10-28T10:00:00Z', scheduledEnd: '2026-10-28T11:00:00Z' })),
      422,
    );
  });

  it('aplica 404 para cuidador inexistente e 400 para fim antes do início', async () => {
    const api = newApi();
    await expectApiError(api.createBooking(walk({ caregiverId: '33333333-3333-3333-3333-333333333339' })), 404);
    await expectApiError(api.createBooking(walk({ scheduledEnd: '2026-10-29T12:00:00Z' })), 400);
  });
});

describe('MockPlantaoPetApi — transições de status', () => {
  it('REQUESTED → ACCEPTED → CANCELLED', async () => {
    const api = newApi();
    const { id } = await api.createBooking(walk());
    expect((await api.acceptBooking(id)).status).toBe('ACCEPTED');
    expect((await api.cancelBooking(id)).status).toBe('CANCELLED');
  });

  it('REQUESTED → REJECTED', async () => {
    const api = newApi();
    const { id } = await api.createBooking(walk());
    expect((await api.rejectBooking(id)).status).toBe('REJECTED');
  });

  it('transições inválidas retornam 409', async () => {
    const api = newApi();
    const { id } = await api.createBooking(walk());
    await api.acceptBooking(id);
    await expectApiError(api.acceptBooking(id), 409);
    await expectApiError(api.rejectBooking(id), 409);
    await api.cancelBooking(id);
    const err = await expectApiError(api.cancelBooking(id), 409);
    expect(err.friendlyMessage).toMatch(/não pode mais ser alterada/);
  });

  it('reserva inexistente retorna 404', async () => {
    await expectApiError(newApi().acceptBooking('nao-existe'), 404);
  });

  it('não vaza referências internas (cópias defensivas)', async () => {
    const api = newApi();
    const booking = await api.createBooking(walk());
    booking.status = 'COMPLETED';
    expect((await api.getBooking(booking.id)).status).toBe('REQUESTED');
  });
});
