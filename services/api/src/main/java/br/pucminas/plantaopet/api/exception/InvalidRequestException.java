package br.pucminas.plantaopet.api.exception;

/** Requisição inválida detectada fora do Bean Validation, associada a um campo (HTTP 400). */
public class InvalidRequestException extends RuntimeException {

    private final String field;

    public InvalidRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
