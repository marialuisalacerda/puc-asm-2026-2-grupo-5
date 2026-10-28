package br.pucminas.plantaopet.api.controller.dto;

import br.pucminas.plantaopet.api.domain.Pet;
import br.pucminas.plantaopet.api.domain.PetSize;

import java.util.UUID;

public record PetResponse(UUID id, UUID tutorId, String name, String species, PetSize size, String notes) {

    public static PetResponse from(Pet pet) {
        return new PetResponse(pet.getId(), pet.getTutorId(), pet.getName(), pet.getSpecies(),
                pet.getSize(), pet.getNotes());
    }
}
