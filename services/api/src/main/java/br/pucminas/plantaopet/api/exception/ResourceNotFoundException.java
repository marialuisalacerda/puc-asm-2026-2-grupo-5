package br.pucminas.plantaopet.api.exception;

import java.util.UUID;

/** Recurso inexistente (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, UUID id) {
        super("%s %s not found".formatted(resource, id));
    }
}
