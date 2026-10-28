import { ApiError } from '../ApiError';
import { HttpPlantaoPetApi, buildQuery, type FetchFn } from '../HttpPlantaoPetApi';

interface FakeResponseInit {
  status?: number;
  body?: unknown;
  contentType?: string;
}

function fakeResponse({ status = 200, body, contentType = 'application/json' }: FakeResponseInit = {}): Response {
  const text = body === undefined ? '' : typeof body === 'string' ? body : JSON.stringify(body);
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: { get: (name: string) => (name.toLowerCase() === 'content-type' ? contentType : null) },
    text: () => Promise.resolve(text),
    json: () => Promise.resolve(JSON.parse(text)),
  } as unknown as Response;
}

function setup(response: Response | Error = fakeResponse({ body: [] })) {
  const fetchFn = jest.fn<ReturnType<FetchFn>, Parameters<FetchFn>>(() =>
    response instanceof Error ? Promise.reject(response) : Promise.resolve(response),
  );
  const api = new HttpPlantaoPetApi({ baseUrl: 'http://api.test:8080/', fetchFn });
  return { api, fetchFn };
}

const BOOKING = {
  id: 'b1',
  tutorId: 't1',
  petId: 'p1',
  caregiverId: 'c1',
  serviceType: 'WALK',
  status: 'REQUESTED',
  scheduledStart: '2026-10-29T13:00:00Z',
  scheduledEnd: '2026-10-29T15:00:00Z',
  priceCents: 6000,
  createdAt: '2026-10-28T12:00:00Z',
  updatedAt: '2026-10-28T12:00:00Z',
};

describe('HttpPlantaoPetApi — requisições', () => {
  it('lista cuidadores ativos em GET /caregivers?active=true (remove barra final da base)', async () => {
    const { api, fetchFn } = setup();
    await api.listCaregivers({ active: true });
    const [url, init] = fetchFn.mock.calls[0];
    expect(url).toBe('http://api.test:8080/caregivers?active=true');
    expect(init?.method).toBe('GET');
    expect(init?.body).toBeUndefined();
  });

  it('omite parâmetros opcionais não informados', async () => {
    const { api, fetchFn } = setup();
    await api.listCaregivers();
    await api.listBookings({ caregiverId: 'c1' });
    expect(fetchFn.mock.calls[0][0]).toBe('http://api.test:8080/caregivers');
    expect(fetchFn.mock.calls[1][0]).toBe('http://api.test:8080/bookings?caregiverId=c1');
  });

  it('monta a query do orçamento com datas codificadas', async () => {
    const { api, fetchFn } = setup(
      fakeResponse({ body: { serviceType: 'WALK', scheduledStart: 'a', scheduledEnd: 'b', priceCents: 6000, currency: 'BRL' } }),
    );
    const quote = await api.quoteBooking({
      serviceType: 'WALK',
      scheduledStart: '2026-10-29T13:00:00.000Z',
      scheduledEnd: '2026-10-29T15:00:00.000Z',
    });
    expect(fetchFn.mock.calls[0][0]).toBe(
      'http://api.test:8080/bookings/quote?serviceType=WALK&scheduledStart=2026-10-29T13%3A00%3A00.000Z&scheduledEnd=2026-10-29T15%3A00%3A00.000Z',
    );
    expect(quote.priceCents).toBe(6000);
  });

  it('envia POST /bookings com JSON e devolve a reserva criada', async () => {
    const { api, fetchFn } = setup(fakeResponse({ status: 201, body: BOOKING }));
    const request = {
      tutorId: 't1',
      petId: 'p1',
      caregiverId: 'c1',
      serviceType: 'WALK' as const,
      scheduledStart: BOOKING.scheduledStart,
      scheduledEnd: BOOKING.scheduledEnd,
    };
    const booking = await api.createBooking(request);
    const [url, init] = fetchFn.mock.calls[0];
    expect(url).toBe('http://api.test:8080/bookings');
    expect(init?.method).toBe('POST');
    expect((init?.headers as Record<string, string>)['Content-Type']).toBe('application/json');
    expect(JSON.parse(init?.body as string)).toEqual(request);
    expect(booking).toEqual(BOOKING);
  });

  it.each([
    ['acceptBooking', '/bookings/b1/accept'],
    ['rejectBooking', '/bookings/b1/reject'],
    ['cancelBooking', '/bookings/b1/cancel'],
  ] as const)('%s usa PATCH %s', async (method, path) => {
    const { api, fetchFn } = setup(fakeResponse({ body: BOOKING }));
    await api[method]('b1');
    expect(fetchFn.mock.calls[0][0]).toBe(`http://api.test:8080${path}`);
    expect(fetchFn.mock.calls[0][1]?.method).toBe('PATCH');
  });

  it('envia motivo no corpo ao recusar', async () => {
    const { api, fetchFn } = setup(fakeResponse({ body: BOOKING }));
    await api.rejectBooking('b1', { reason: 'Agenda cheia' });
    expect(JSON.parse(fetchFn.mock.calls[0][1]?.body as string)).toEqual({ reason: 'Agenda cheia' });
  });

  it('monta rotas de tutor e pets', async () => {
    const { api, fetchFn } = setup(fakeResponse({ body: [] }));
    await api.listTutorPets('11111111-1111-1111-1111-111111111111');
    expect(fetchFn.mock.calls[0][0]).toBe('http://api.test:8080/tutors/11111111-1111-1111-1111-111111111111/pets');
  });
});

describe('HttpPlantaoPetApi — erros (RFC 7807)', () => {
  it('converte Problem 409 da criação em ApiError com mensagem amigável', async () => {
    const problem = { type: 'about:blank', title: 'Conflict', status: 409, detail: 'Caregiver already booked' };
    const { api } = setup(fakeResponse({ status: 409, body: problem, contentType: 'application/problem+json' }));
    const error = await api.createBooking({} as never).catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    const apiError = error as ApiError;
    expect(apiError.status).toBe(409);
    expect(apiError.operation).toBe('createBooking');
    expect(apiError.problem).toMatchObject(problem);
    expect(apiError.friendlyMessage).toBe('Cuidador já tem reserva nesse horário.');
  });

  it('lista erros de campo no 400', async () => {
    const problem = { status: 400, title: 'Bad Request', errors: [{ field: 'name', message: 'must not be blank' }] };
    const { api } = setup(fakeResponse({ status: 400, body: problem, contentType: 'application/problem+json' }));
    const error = (await api.createPet({ tutorId: 't', name: '', species: 'DOG' }).catch((e: unknown) => e)) as ApiError;
    expect(error.friendlyMessage).toBe('Dados inválidos. name: must not be blank.');
  });

  it('usa detail do Problem no 422', async () => {
    const { api } = setup(fakeResponse({ status: 422, body: { status: 422, detail: 'Início no passado' } }));
    const error = (await api.createBooking({} as never).catch((e: unknown) => e)) as ApiError;
    expect(error.friendlyMessage).toBe('Início no passado');
  });

  it('tolera corpo vazio ou não-JSON', async () => {
    const { api } = setup(fakeResponse({ status: 503, body: '<html>indisponível</html>', contentType: 'text/html' }));
    const error = (await api.listTutors().catch((e: unknown) => e)) as ApiError;
    expect(error.status).toBe(503);
    expect(error.friendlyMessage).toMatch(/servidor/);
  });

  it('converte falha de rede em ApiError status 0', async () => {
    const { api } = setup(new TypeError('Network request failed'));
    const error = (await api.listTutors().catch((e: unknown) => e)) as ApiError;
    expect(error).toBeInstanceOf(ApiError);
    expect(error.isNetworkError).toBe(true);
    expect(error.friendlyMessage).toMatch(/Não foi possível conectar/);
  });
});

describe('buildQuery', () => {
  it('ignora valores indefinidos', () => {
    expect(buildQuery({ a: undefined, b: false, c: 'x y' })).toBe('?b=false&c=x%20y');
    expect(buildQuery({})).toBe('');
  });
});
