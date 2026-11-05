package br.pucminas.plantaopet.api.exception;

/** Regra de negócio violada com dados sintaticamente válidos (HTTP 422). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
