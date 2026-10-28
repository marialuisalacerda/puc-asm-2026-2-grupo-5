import type { OperationId, Problem } from './types';

/** Status 0 representa falha de rede (a requisição não chegou a ter resposta HTTP). */
export const NETWORK_ERROR_STATUS = 0;

const TRANSITIONS: OperationId[] = ['acceptBooking', 'rejectBooking', 'cancelBooking'];

/**
 * Erro tipado da API, construído a partir de um Problem (RFC 7807).
 * `friendlyMessage` traz um texto em português adequado para exibir ao usuário.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly problem: Problem;
  readonly operation?: OperationId;

  constructor(status: number, problem: Problem = {}, operation?: OperationId) {
    super(problem.detail ?? problem.title ?? `Erro HTTP ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.problem = problem;
    this.operation = operation;
    // Mantém instanceof funcionando quando transpilado para ES5.
    Object.setPrototypeOf(this, ApiError.prototype);
  }

  get isNetworkError(): boolean {
    return this.status === NETWORK_ERROR_STATUS;
  }

  get friendlyMessage(): string {
    return friendlyMessageFor(this.status, this.problem, this.operation);
  }
}

export function friendlyMessageFor(status: number, problem: Problem, operation?: OperationId): string {
  if (status === NETWORK_ERROR_STATUS) {
    return 'Não foi possível conectar à API. Verifique sua conexão e o endereço configurado.';
  }

  if (status === 400) {
    const fields = (problem.errors ?? [])
      .map((e) => [e.field, e.message].filter(Boolean).join(': '))
      .filter((s) => s.length > 0);
    if (fields.length > 0) return `Dados inválidos. ${fields.join('; ')}.`;
    return problem.detail ? `Dados inválidos: ${problem.detail}` : 'Dados inválidos. Revise os campos e tente novamente.';
  }

  if (status === 404) {
    return problem.detail ?? 'Registro não encontrado. Atualize a tela e tente novamente.';
  }

  if (status === 409) {
    if (operation === 'createBooking') return 'Cuidador já tem reserva nesse horário.';
    if (operation === 'createTutor') return 'Já existe um cadastro com este e-mail.';
    if (operation && TRANSITIONS.includes(operation)) {
      return 'Esta reserva não pode mais ser alterada no status atual. Atualize a lista.';
    }
    return problem.detail ?? 'Conflito com o estado atual dos dados.';
  }

  if (status === 422) {
    return problem.detail ?? 'A solicitação viola uma regra de negócio.';
  }

  if (status >= 500) {
    return 'O servidor encontrou um erro. Tente novamente em instantes.';
  }

  return problem.detail ?? problem.title ?? `Erro inesperado (HTTP ${status}).`;
}

/** Converte qualquer erro em mensagem amigável para a interface. */
export function toUserMessage(error: unknown): string {
  if (error instanceof ApiError) return error.friendlyMessage;
  return 'Ocorreu um erro inesperado. Tente novamente.';
}
