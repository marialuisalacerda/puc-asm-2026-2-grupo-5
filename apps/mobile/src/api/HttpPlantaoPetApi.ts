import { ApiError, NETWORK_ERROR_STATUS } from './ApiError';
import type {
  Booking,
  Caregiver,
  CreateBookingRequest,
  CreatePetRequest,
  CreateTutorRequest,
  ListBookingsParams,
  ListCaregiversParams,
  OperationId,
  Pet,
  PlantaoPetApi,
  PriceQuote,
  Problem,
  QuoteBookingParams,
  ReasonRequest,
  Tutor,
  Uuid,
} from './types';

export type FetchFn = (input: string, init?: RequestInit) => Promise<Response>;

export interface HttpPlantaoPetApiOptions {
  baseUrl: string;
  /** Permite injetar um fetch falso nos testes. Padrão: fetch global. */
  fetchFn?: FetchFn;
}

type Query = Record<string, string | number | boolean | undefined>;

/** Implementação HTTP/JSON do contrato docs/api/openapi.yaml. */
export class HttpPlantaoPetApi implements PlantaoPetApi {
  private readonly baseUrl: string;
  private readonly fetchFn: FetchFn;

  constructor(options: HttpPlantaoPetApiOptions) {
    this.baseUrl = options.baseUrl.replace(/\/+$/, '');
    this.fetchFn = options.fetchFn ?? ((input, init) => fetch(input, init));
  }

  listTutors(): Promise<Tutor[]> {
    return this.request('listTutors', 'GET', '/tutors');
  }

  getTutor(tutorId: Uuid): Promise<Tutor> {
    return this.request('getTutor', 'GET', `/tutors/${enc(tutorId)}`);
  }

  createTutor(request: CreateTutorRequest): Promise<Tutor> {
    return this.request('createTutor', 'POST', '/tutors', { body: request });
  }

  listTutorPets(tutorId: Uuid): Promise<Pet[]> {
    return this.request('listTutorPets', 'GET', `/tutors/${enc(tutorId)}/pets`);
  }

  getPet(petId: Uuid): Promise<Pet> {
    return this.request('getPet', 'GET', `/pets/${enc(petId)}`);
  }

  createPet(request: CreatePetRequest): Promise<Pet> {
    return this.request('createPet', 'POST', '/pets', { body: request });
  }

  listCaregivers(params: ListCaregiversParams = {}): Promise<Caregiver[]> {
    return this.request('listCaregivers', 'GET', '/caregivers', { query: { active: params.active } });
  }

  getCaregiver(caregiverId: Uuid): Promise<Caregiver> {
    return this.request('getCaregiver', 'GET', `/caregivers/${enc(caregiverId)}`);
  }

  quoteBooking(params: QuoteBookingParams): Promise<PriceQuote> {
    return this.request('quoteBooking', 'GET', '/bookings/quote', {
      query: {
        serviceType: params.serviceType,
        scheduledStart: params.scheduledStart,
        scheduledEnd: params.scheduledEnd,
      },
    });
  }

  createBooking(request: CreateBookingRequest): Promise<Booking> {
    return this.request('createBooking', 'POST', '/bookings', { body: request });
  }

  listBookings(params: ListBookingsParams = {}): Promise<Booking[]> {
    return this.request('listBookings', 'GET', '/bookings', {
      query: { tutorId: params.tutorId, caregiverId: params.caregiverId, status: params.status },
    });
  }

  getBooking(bookingId: Uuid): Promise<Booking> {
    return this.request('getBooking', 'GET', `/bookings/${enc(bookingId)}`);
  }

  acceptBooking(bookingId: Uuid): Promise<Booking> {
    return this.request('acceptBooking', 'PATCH', `/bookings/${enc(bookingId)}/accept`);
  }

  rejectBooking(bookingId: Uuid, request?: ReasonRequest): Promise<Booking> {
    return this.request('rejectBooking', 'PATCH', `/bookings/${enc(bookingId)}/reject`, { body: request });
  }

  cancelBooking(bookingId: Uuid, request?: ReasonRequest): Promise<Booking> {
    return this.request('cancelBooking', 'PATCH', `/bookings/${enc(bookingId)}/cancel`, { body: request });
  }

  private async request<T>(
    operation: OperationId,
    method: 'GET' | 'POST' | 'PATCH',
    path: string,
    options: { query?: Query; body?: unknown } = {},
  ): Promise<T> {
    const url = `${this.baseUrl}${path}${buildQuery(options.query)}`;
    const headers: Record<string, string> = { Accept: 'application/json, application/problem+json' };
    const init: RequestInit = { method, headers };
    if (options.body !== undefined) {
      headers['Content-Type'] = 'application/json';
      init.body = JSON.stringify(options.body);
    }

    let response: Response;
    try {
      response = await this.fetchFn(url, init);
    } catch (cause) {
      const detail = cause instanceof Error ? cause.message : String(cause);
      throw new ApiError(NETWORK_ERROR_STATUS, { title: 'Network error', detail }, operation);
    }

    if (!response.ok) {
      throw new ApiError(response.status, await readProblem(response), operation);
    }

    const text = await response.text();
    return (text ? JSON.parse(text) : undefined) as T;
  }
}

function enc(value: string): string {
  return encodeURIComponent(value);
}

export function buildQuery(query?: Query): string {
  if (!query) return '';
  const parts = Object.entries(query)
    .filter(([, v]) => v !== undefined && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`);
  return parts.length ? `?${parts.join('&')}` : '';
}

async function readProblem(response: Response): Promise<Problem> {
  try {
    const text = await response.text();
    if (!text) return { status: response.status };
    const parsed: unknown = JSON.parse(text);
    if (parsed && typeof parsed === 'object') return { status: response.status, ...(parsed as Problem) };
    return { status: response.status, detail: String(parsed) };
  } catch {
    return { status: response.status };
  }
}
