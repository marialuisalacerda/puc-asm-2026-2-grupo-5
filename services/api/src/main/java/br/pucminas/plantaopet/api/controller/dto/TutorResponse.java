package br.pucminas.plantaopet.api.controller.dto;

import br.pucminas.plantaopet.api.domain.Tutor;

import java.util.UUID;

public record TutorResponse(UUID id, String name, String email) {

    public static TutorResponse from(Tutor tutor) {
        return new TutorResponse(tutor.getId(), tutor.getName(), tutor.getEmail());
    }
}
