/**
 * Tipos do contrato REST (docs/api/openapi.yaml, versão 1.0.0).
 * Devem refletir exatamente os schemas do OpenAPI: alterar aqui somente quando o contrato mudar.
 */

/** Identificador UUID (format: uuid). */
export type Uuid = string;

/** Data/hora ISO-8601 (format: date-time). */
export type IsoDateTime = string;

export const SERVICE_TYPES = ['WALK', 'COMPANIONSHIP', 'HOME_VISIT', 'HOSTING', 'TEMPORARY_CARE'] as const;
export type ServiceType = (typeof SERVICE_TYPES)[number];

export const BOOKING_STATUSES = [
  'REQUESTED',
  'ACCEPTED',
  'PAID',
  'IN_PROGRESS',
  'COMPLETED',
  'REJECTED',
  'CANCELLED',
  'EXPIRED',
] as const;
export type BookingStatus = (typeof BOOKING_STATUSES)[number];

export const PET_SIZES = ['SMALL', 'MEDIUM', 'LARGE'] as const;
export type PetSize = (typeof PET_SIZES)[number];

export interface Tutor {
  id: Uuid;
  name: string;
  email: string;
}

export interface CreateTutorRequest {
  name: string;
  email: string;
}

export interface Pet {
  id: Uuid;
  tutorId: Uuid;
  name: string;
  species: string;
  size?: PetSize;
  notes?: string | null;
}

export interface CreatePetRequest {
  tutorId: Uuid;
  name: string;
  species: string;
  size?: PetSize;
  notes?: string | null;
}

export interface Caregiver {
  id: Uuid;
  name: string;
  email: string;
  active: boolean;
}

export interface PriceQuote {
  serviceType: ServiceType;
  scheduledStart: IsoDateTime;
  scheduledEnd: IsoDateTime;
  priceCents: number;
  currency: string;
}

export interface QuoteBookingParams {
  serviceType: ServiceType;
  scheduledStart: IsoDateTime;
  scheduledEnd: IsoDateTime;
}

export interface CreateBookingRequest {
  tutorId: Uuid;
  petId: Uuid;
  caregiverId: Uuid;
  serviceType: ServiceType;
  scheduledStart: IsoDateTime;
  scheduledEnd: IsoDateTime;
}

export interface Booking {
  id: Uuid;
  tutorId: Uuid;
  petId: Uuid;
  caregiverId: Uuid;
  serviceType: ServiceType;
  status: BookingStatus;
  scheduledStart: IsoDateTime;
  scheduledEnd: IsoDateTime;
  priceCents: number;
  createdAt: IsoDateTime;
  updatedAt: IsoDateTime;
}

export interface ListBookingsParams {
  tutorId?: Uuid;
  caregiverId?: Uuid;
  status?: BookingStatus;
}

export interface ListCaregiversParams {
  /** Padrão do contrato: true (somente ativos). */
  active?: boolean;
}

export interface ReasonRequest {
  reason?: string;
}

/** Erro RFC 7807 (application/problem+json). */
export interface Problem {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  /** Erros de validação por campo (apenas 400). */
  errors?: { field?: string; message?: string }[];
}

/** Nome da operação (operationId do OpenAPI), usado para mensagens de erro contextualizadas. */
export type OperationId =
  | 'listTutors'
  | 'createTutor'
  | 'getTutor'
  | 'listTutorPets'
  | 'createPet'
  | 'getPet'
  | 'listCaregivers'
  | 'getCaregiver'
  | 'quoteBooking'
  | 'createBooking'
  | 'listBookings'
  | 'getBooking'
  | 'acceptBooking'
  | 'rejectBooking'
  | 'cancelBooking';

/**
 * Porta de acesso à API. As telas dependem apenas desta interface;
 * implementações: HttpPlantaoPetApi (API real) e MockPlantaoPetApi (dados em memória).
 */
export interface PlantaoPetApi {
  listTutors(): Promise<Tutor[]>;
  getTutor(tutorId: Uuid): Promise<Tutor>;
  createTutor(request: CreateTutorRequest): Promise<Tutor>;

  listTutorPets(tutorId: Uuid): Promise<Pet[]>;
  getPet(petId: Uuid): Promise<Pet>;
  createPet(request: CreatePetRequest): Promise<Pet>;

  listCaregivers(params?: ListCaregiversParams): Promise<Caregiver[]>;
  getCaregiver(caregiverId: Uuid): Promise<Caregiver>;

  quoteBooking(params: QuoteBookingParams): Promise<PriceQuote>;
  createBooking(request: CreateBookingRequest): Promise<Booking>;
  listBookings(params?: ListBookingsParams): Promise<Booking[]>;
  getBooking(bookingId: Uuid): Promise<Booking>;
  acceptBooking(bookingId: Uuid): Promise<Booking>;
  rejectBooking(bookingId: Uuid, request?: ReasonRequest): Promise<Booking>;
  cancelBooking(bookingId: Uuid, request?: ReasonRequest): Promise<Booking>;
}
