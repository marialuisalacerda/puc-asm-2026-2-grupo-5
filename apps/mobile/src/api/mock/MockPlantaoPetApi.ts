import { ApiError } from '../ApiError';
import {
  BOOKING_STATUSES,
  PET_SIZES,
  SERVICE_TYPES,
  type Booking,
  type BookingStatus,
  type Caregiver,
  type CreateBookingRequest,
  type CreatePetRequest,
  type CreateTutorRequest,
  type ListBookingsParams,
  type ListCaregiversParams,
  type OperationId,
  type Pet,
  type PlantaoPetApi,
  type PriceQuote,
  type QuoteBookingParams,
  type Tutor,
  type Uuid,
} from '../types';
import { calculatePriceCents } from './pricing';
import { SEED_CAREGIVERS, SEED_PETS, SEED_TUTORS } from './seeds';

export interface MockPlantaoPetApiOptions {
  /** Latência simulada por chamada, em ms (padrão 400; use 0 nos testes). */
  latencyMs?: number;
  /** Relógio injetável para testar regras de "início no futuro". */
  now?: () => Date;
  /** Gerador de IDs injetável. */
  idFactory?: () => Uuid;
  tutors?: Tutor[];
  pets?: Pet[];
  caregivers?: Caregiver[];
  bookings?: Booking[];
}

/** Transições permitidas pelo contrato; qualquer outra resulta em 409. */
const ALLOWED_TRANSITIONS: Record<'accept' | 'reject' | 'cancel', { from: BookingStatus[]; to: BookingStatus }> = {
  accept: { from: ['REQUESTED'], to: 'ACCEPTED' },
  reject: { from: ['REQUESTED'], to: 'REJECTED' },
  cancel: { from: ['REQUESTED', 'ACCEPTED'], to: 'CANCELLED' },
};

const BLOCKING_STATUSES: BookingStatus[] = ['REQUESTED', 'ACCEPTED'];

const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value)) as T;

export function randomUuid(): Uuid {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = Math.floor(Math.random() * 16);
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
  });
}

/**
 * Implementação em memória do contrato, com os seeds compartilhados com a API
 * e as mesmas regras de negócio e preço de docs/architecture/contratos-integracao.md.
 * Permite rodar o app sem backend (EXPO_PUBLIC_USE_MOCKS=true).
 */
export class MockPlantaoPetApi implements PlantaoPetApi {
  private readonly latencyMs: number;
  private readonly now: () => Date;
  private readonly idFactory: () => Uuid;
  private readonly tutors: Tutor[];
  private readonly pets: Pet[];
  private readonly caregivers: Caregiver[];
  private readonly bookings: Booking[];

  constructor(options: MockPlantaoPetApiOptions = {}) {
    this.latencyMs = options.latencyMs ?? 400;
    this.now = options.now ?? (() => new Date());
    this.idFactory = options.idFactory ?? randomUuid;
    this.tutors = clone(options.tutors ?? SEED_TUTORS);
    this.pets = clone(options.pets ?? SEED_PETS);
    this.caregivers = clone(options.caregivers ?? SEED_CAREGIVERS);
    this.bookings = clone(options.bookings ?? []);
  }

  // ---------- Tutores ----------

  listTutors(): Promise<Tutor[]> {
    return this.respond(() => this.tutors);
  }

  getTutor(tutorId: Uuid): Promise<Tutor> {
    return this.respond(() => this.findTutor(tutorId, 'getTutor'));
  }

  createTutor(request: CreateTutorRequest): Promise<Tutor> {
    return this.respond(() => {
      const op: OperationId = 'createTutor';
      const name = request.name?.trim() ?? '';
      const email = request.email?.trim() ?? '';
      const errors = [];
      if (!name || name.length > 120) errors.push({ field: 'name', message: 'deve ter entre 1 e 120 caracteres' });
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 254) {
        errors.push({ field: 'email', message: 'deve ser um e-mail válido' });
      }
      if (errors.length) throw badRequest(op, errors);
      if (this.tutors.some((t) => t.email.toLowerCase() === email.toLowerCase())) {
        throw problem(409, 'Conflict', 'Já existe um tutor com este e-mail.', op);
      }
      const tutor: Tutor = { id: this.idFactory(), name, email };
      this.tutors.push(tutor);
      return tutor;
    });
  }

  // ---------- Pets ----------

  listTutorPets(tutorId: Uuid): Promise<Pet[]> {
    return this.respond(() => {
      this.findTutor(tutorId, 'listTutorPets');
      return this.pets.filter((p) => p.tutorId === tutorId);
    });
  }

  getPet(petId: Uuid): Promise<Pet> {
    return this.respond(() => this.findPet(petId, 'getPet'));
  }

  createPet(request: CreatePetRequest): Promise<Pet> {
    return this.respond(() => {
      const op: OperationId = 'createPet';
      const name = request.name?.trim() ?? '';
      const species = request.species?.trim() ?? '';
      const errors = [];
      if (!request.tutorId) errors.push({ field: 'tutorId', message: 'é obrigatório' });
      if (!name || name.length > 80) errors.push({ field: 'name', message: 'deve ter entre 1 e 80 caracteres' });
      if (!species || species.length > 40) errors.push({ field: 'species', message: 'deve ter entre 1 e 40 caracteres' });
      if (request.size !== undefined && !PET_SIZES.includes(request.size)) {
        errors.push({ field: 'size', message: 'valor inválido' });
      }
      if (request.notes && request.notes.length > 500) errors.push({ field: 'notes', message: 'máximo de 500 caracteres' });
      if (errors.length) throw badRequest(op, errors);
      this.findTutor(request.tutorId, op);
      const pet: Pet = {
        id: this.idFactory(),
        tutorId: request.tutorId,
        name,
        species,
        size: request.size,
        notes: request.notes?.trim() ? request.notes.trim() : null,
      };
      this.pets.push(pet);
      return pet;
    });
  }

  // ---------- Cuidadores ----------

  listCaregivers(params: ListCaregiversParams = {}): Promise<Caregiver[]> {
    const active = params.active ?? true;
    return this.respond(() => this.caregivers.filter((c) => c.active === active));
  }

  getCaregiver(caregiverId: Uuid): Promise<Caregiver> {
    return this.respond(() => this.findCaregiver(caregiverId, 'getCaregiver'));
  }

  // ---------- Reservas ----------

  quoteBooking(params: QuoteBookingParams): Promise<PriceQuote> {
    return this.respond(() => {
      const { start, end } = this.validatePeriod(params, 'quoteBooking');
      return {
        serviceType: params.serviceType,
        scheduledStart: params.scheduledStart,
        scheduledEnd: params.scheduledEnd,
        priceCents: calculatePriceCents(params.serviceType, start, end),
        currency: 'BRL',
      };
    });
  }

  createBooking(request: CreateBookingRequest): Promise<Booking> {
    return this.respond(() => {
      const op: OperationId = 'createBooking';
      const missing = (['tutorId', 'petId', 'caregiverId', 'serviceType', 'scheduledStart', 'scheduledEnd'] as const)
        .filter((f) => !request[f])
        .map((field) => ({ field, message: 'é obrigatório' }));
      if (missing.length) throw badRequest(op, missing);

      // Regra 1a: fim depois do início (400).
      const { start, end } = this.validatePeriod(request, op);

      // Regra 2: existência (404).
      this.findTutor(request.tutorId, op);
      const pet = this.findPet(request.petId, op);
      const caregiver = this.findCaregiver(request.caregiverId, op);

      // Regras 1b e 2: regras de negócio (422).
      if (pet.tutorId !== request.tutorId) {
        throw problem(422, 'Unprocessable Entity', 'O pet informado não pertence a este tutor.', op);
      }
      if (!caregiver.active) {
        throw problem(422, 'Unprocessable Entity', 'Este cuidador está inativo e não aceita reservas.', op);
      }
      const now = this.now();
      if (start.getTime() <= now.getTime()) {
        throw problem(422, 'Unprocessable Entity', 'O início da reserva deve ser no futuro.', op);
      }

      // Regra 3: sem sobreposição com reservas REQUESTED/ACCEPTED do cuidador (409).
      const overlaps = this.bookings.some(
        (b) =>
          b.caregiverId === request.caregiverId &&
          BLOCKING_STATUSES.includes(b.status) &&
          new Date(b.scheduledStart).getTime() < end.getTime() &&
          start.getTime() < new Date(b.scheduledEnd).getTime(),
      );
      if (overlaps) throw problem(409, 'Conflict', 'Cuidador já tem reserva nesse horário.', op);

      const timestamp = now.toISOString();
      const booking: Booking = {
        id: this.idFactory(),
        tutorId: request.tutorId,
        petId: request.petId,
        caregiverId: request.caregiverId,
        serviceType: request.serviceType,
        status: 'REQUESTED',
        scheduledStart: start.toISOString(),
        scheduledEnd: end.toISOString(),
        priceCents: calculatePriceCents(request.serviceType, start, end),
        createdAt: timestamp,
        updatedAt: timestamp,
      };
      this.bookings.push(booking);
      return booking;
    });
  }

  listBookings(params: ListBookingsParams = {}): Promise<Booking[]> {
    return this.respond(() => {
      if (params.status && !BOOKING_STATUSES.includes(params.status)) {
        throw badRequest('listBookings', [{ field: 'status', message: 'valor inválido' }]);
      }
      return this.bookings
        .filter((b) => !params.tutorId || b.tutorId === params.tutorId)
        .filter((b) => !params.caregiverId || b.caregiverId === params.caregiverId)
        .filter((b) => !params.status || b.status === params.status)
        .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
    });
  }

  getBooking(bookingId: Uuid): Promise<Booking> {
    return this.respond(() => this.findBooking(bookingId, 'getBooking'));
  }

  acceptBooking(bookingId: Uuid): Promise<Booking> {
    return this.respond(() => this.transition(bookingId, 'accept', 'acceptBooking'));
  }

  rejectBooking(bookingId: Uuid): Promise<Booking> {
    return this.respond(() => this.transition(bookingId, 'reject', 'rejectBooking'));
  }

  cancelBooking(bookingId: Uuid): Promise<Booking> {
    return this.respond(() => this.transition(bookingId, 'cancel', 'cancelBooking'));
  }

  // ---------- Auxiliares ----------

  private transition(bookingId: Uuid, action: keyof typeof ALLOWED_TRANSITIONS, op: OperationId): Booking {
    const booking = this.findBooking(bookingId, op);
    const rule = ALLOWED_TRANSITIONS[action];
    if (!rule.from.includes(booking.status)) {
      throw problem(409, 'Conflict', `Transição inválida: ${booking.status} → ${rule.to}.`, op);
    }
    booking.status = rule.to;
    booking.updatedAt = this.now().toISOString();
    return booking;
  }

  private validatePeriod(params: { serviceType: string; scheduledStart: string; scheduledEnd: string }, op: OperationId) {
    const errors = [];
    if (!SERVICE_TYPES.includes(params.serviceType as never)) {
      errors.push({ field: 'serviceType', message: 'valor inválido' });
    }
    const start = new Date(params.scheduledStart);
    const end = new Date(params.scheduledEnd);
    if (Number.isNaN(start.getTime())) errors.push({ field: 'scheduledStart', message: 'data/hora inválida' });
    if (Number.isNaN(end.getTime())) errors.push({ field: 'scheduledEnd', message: 'data/hora inválida' });
    if (errors.length) throw badRequest(op, errors);
    if (end.getTime() <= start.getTime()) {
      throw badRequest(op, [{ field: 'scheduledEnd', message: 'deve ser posterior ao início' }]);
    }
    return { start, end };
  }

  private findTutor(id: Uuid, op: OperationId): Tutor {
    return orNotFound(this.tutors.find((t) => t.id === id), 'Tutor', op);
  }

  private findPet(id: Uuid, op: OperationId): Pet {
    return orNotFound(this.pets.find((p) => p.id === id), 'Pet', op);
  }

  private findCaregiver(id: Uuid, op: OperationId): Caregiver {
    return orNotFound(this.caregivers.find((c) => c.id === id), 'Cuidador', op);
  }

  private findBooking(id: Uuid, op: OperationId): Booking {
    return orNotFound(this.bookings.find((b) => b.id === id), 'Reserva', op);
  }

  /** Executa a operação após a latência simulada e devolve uma cópia (evita mutação externa do estado). */
  private respond<T>(operation: () => T): Promise<T> {
    return new Promise<T>((resolve, reject) => {
      const run = () => {
        try {
          resolve(clone(operation()));
        } catch (error) {
          reject(error);
        }
      };
      if (this.latencyMs > 0) setTimeout(run, this.latencyMs);
      else run();
    });
  }
}

function problem(status: number, title: string, detail: string, op: OperationId): ApiError {
  return new ApiError(status, { type: 'about:blank', title, status, detail }, op);
}

function badRequest(op: OperationId, errors: { field: string; message: string }[]): ApiError {
  return new ApiError(400, { type: 'about:blank', title: 'Bad Request', status: 400, detail: 'Requisição inválida.', errors }, op);
}

function orNotFound<T>(value: T | undefined, entity: string, op: OperationId): T {
  if (value === undefined) throw problem(404, 'Not Found', `${entity} não encontrado(a).`, op);
  return value;
}
