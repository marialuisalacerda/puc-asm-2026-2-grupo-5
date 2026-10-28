package br.pucminas.plantaopet.api.exception;

/** Conflito com o estado atual (horário ocupado, e-mail duplicado) (HTTP 409). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
