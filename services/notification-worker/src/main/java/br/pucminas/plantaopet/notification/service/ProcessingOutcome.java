package br.pucminas.plantaopet.notification.service;

/** Resultado do processamento de um evento (valor da tag {@code outcome}). */
public enum ProcessingOutcome {
    PROCESSED("processed"),
    DUPLICATE("duplicate");

    private final String tagValue;

    ProcessingOutcome(String tagValue) {
        this.tagValue = tagValue;
    }

    public String tagValue() {
        return tagValue;
    }
}
